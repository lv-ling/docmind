from __future__ import annotations

import json
import re
from collections import OrderedDict
from datetime import date, datetime

import re2  # type: ignore[import-untyped]
from pydantic import JsonValue

from docmind_ai.contracts.extraction import (
    AiExtractionRequest,
    ModelCandidateOutput,
    ModelEvidence,
    ModelExtractionOutput,
    ModelFieldOutput,
    TokenizedDocumentNode,
)
from docmind_ai.contracts.schema import (
    LiteralSchemaFieldDefault,
    SchemaFieldDefinition,
    SchemaValueType,
)
from docmind_ai.extraction.providers.base import ProviderResult

PROMPT_VERSION = "mock-key-value-v1"


class MockExtractionProvider:
    async def extract(self, request: AiExtractionRequest) -> ProviderResult:
        data: dict[str, JsonValue] = {}
        field_outputs: list[ModelFieldOutput] = []
        for field in sorted(request.fields, key=lambda item: item.position):
            matches = self._find_candidates(field, request.document.nodes)
            unique_matches = self._deduplicate(matches)
            if unique_matches:
                selected_value, selected_evidence = unique_matches[0]
                ambiguous = len(unique_matches) > 1
                confidence = 0.76 if ambiguous else 0.96
                candidates = (
                    [
                        ModelCandidateOutput(
                            value=value,
                            confidence=0.76,
                            evidence=[evidence],
                        )
                        for value, evidence in unique_matches
                    ]
                    if ambiguous
                    else []
                )
                field_output = ModelFieldOutput(
                    path=field.json_path,
                    value=selected_value,
                    confidence=confidence,
                    evidence=[selected_evidence],
                    candidates=candidates,
                    needs_review=ambiguous,
                )
            elif isinstance(field.default, LiteralSchemaFieldDefault):
                field_output = ModelFieldOutput(
                    path=field.json_path,
                    value=field.default.value,
                    confidence=None,
                    evidence=[],
                    candidates=[],
                    needs_review=False,
                )
            else:
                field_output = ModelFieldOutput(
                    path=field.json_path,
                    value=None,
                    confidence=None,
                    evidence=[],
                    candidates=[],
                    needs_review=field.required,
                )
            self._set_json_path(data, field.json_path, field_output.value)
            field_outputs.append(field_output)

        output = ModelExtractionOutput(data=data, fields=field_outputs)
        serialized_output = output.model_dump_json()
        return ProviderResult(
            output=output,
            provider="mock",
            model="deterministic-key-value",
            prompt_version=PROMPT_VERSION,
            input_tokens=_estimate_tokens(request.model_dump_json()),
            output_tokens=_estimate_tokens(serialized_output),
        )

    def _find_candidates(
        self,
        field: SchemaFieldDefinition,
        nodes: list[TokenizedDocumentNode],
    ) -> list[tuple[JsonValue, ModelEvidence]]:
        labels = self._field_labels(field)
        results: list[tuple[JsonValue, ModelEvidence]] = []
        for node in nodes:
            for label in labels:
                pattern = re2.compile(
                    re2.escape(label) + r"\s*[:：=]\s*([^\r\n;,；，]+)",  # noqa: RUF001
                    options=_case_insensitive_options(),
                )
                for match in pattern.finditer(node.tokenized_text):
                    raw_value = match.group(1).strip()
                    value = self._coerce(raw_value, field.value_type)
                    if value is None and not field.nullable:
                        continue
                    if not self._within_constraints(value, field):
                        continue
                    results.append(
                        (
                            value,
                            ModelEvidence(
                                node_id=node.node_id,
                                page_number=node.page_number,
                                tokenized_text=node.tokenized_text[:10_000],
                            ),
                        )
                    )
        return results

    @staticmethod
    def _field_labels(field: SchemaFieldDefinition) -> list[str]:
        labels = [field.key]
        description = field.description.strip().rstrip(":：")  # noqa: RUF001
        if description and len(description) <= 100:
            labels.append(description)
        if field.extraction_hint:
            hint = field.extraction_hint.strip()
            if hint.lower().startswith("label:"):
                labels.append(hint.split(":", 1)[1].strip())
        return list(OrderedDict.fromkeys(label for label in labels if label))

    @staticmethod
    def _coerce(raw_value: str, value_type: SchemaValueType) -> JsonValue:
        value = raw_value.strip().strip("\"'")
        if value.casefold() in {"null", "none", "无", "空"}:
            return None
        if value_type == "string":
            return value
        if value_type in {"number", "integer"}:
            match = re.search(r"[-+]?\d[\d,]*(?:\.\d+)?", value)
            if not match:
                return None
            numeric = match.group(0).replace(",", "")
            try:
                return int(float(numeric)) if value_type == "integer" else float(numeric)
            except ValueError:
                return None
        if value_type == "boolean":
            normalized = value.casefold()
            if normalized in {"true", "yes", "y", "1", "是", "有"}:
                return True
            if normalized in {"false", "no", "n", "0", "否", "无"}:
                return False
            return None
        if value_type == "date":
            normalized_date = value.replace("年", "-").replace("月", "-").replace("日", "")
            try:
                return date.fromisoformat(normalized_date).isoformat()
            except ValueError:
                return None
        if value_type == "datetime":
            try:
                return datetime.fromisoformat(value.replace("Z", "+00:00")).isoformat()
            except ValueError:
                return None
        if value_type in {"object", "array"}:
            try:
                decoded: JsonValue = json.loads(value)
            except json.JSONDecodeError:
                return None
            if value_type == "object" and not isinstance(decoded, dict):
                return None
            if value_type == "array" and not isinstance(decoded, list):
                return None
            return decoded
        return None

    @staticmethod
    def _within_constraints(value: JsonValue, field: SchemaFieldDefinition) -> bool:
        constraints = field.constraints
        if value is None:
            return field.nullable
        if constraints.enum_values and value not in constraints.enum_values:
            return False
        if isinstance(value, str):
            if constraints.min_length is not None and len(value) < constraints.min_length:
                return False
            if constraints.max_length is not None and len(value) > constraints.max_length:
                return False
        if isinstance(value, (int, float)) and not isinstance(value, bool):
            if constraints.minimum is not None and value < constraints.minimum:
                return False
            if constraints.maximum is not None and value > constraints.maximum:
                return False
        return True

    @staticmethod
    def _deduplicate(
        values: list[tuple[JsonValue, ModelEvidence]],
    ) -> list[tuple[JsonValue, ModelEvidence]]:
        unique: OrderedDict[str, tuple[JsonValue, ModelEvidence]] = OrderedDict()
        for value, evidence in values:
            key = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
            unique.setdefault(key, (value, evidence))
        return list(unique.values())[:20]

    @staticmethod
    def _set_json_path(data: dict[str, JsonValue], path: str, value: JsonValue) -> None:
        parts = path.removeprefix("$.").split(".")
        current: dict[str, JsonValue] = data
        for part in parts[:-1]:
            child = current.get(part)
            if not isinstance(child, dict):
                child = {}
                current[part] = child
            current = child
        current[parts[-1]] = value


def _case_insensitive_options() -> object:
    options = re2.Options()
    options.case_sensitive = False
    return options


def _estimate_tokens(text: str) -> int:
    return max(1, (len(text) + 3) // 4)
