from __future__ import annotations

import json
import re
from collections.abc import Mapping
from typing import cast

from jsonschema import Draft202012Validator  # type: ignore[import-untyped]
from jsonschema.exceptions import SchemaError  # type: ignore[import-untyped]
from pydantic import JsonValue

from docmind_ai.contracts.extraction import (
    AiExtractionRequest,
    ModelEvidence,
    ModelExtractionOutput,
    TokenizedDocumentNode,
)
from docmind_ai.extraction.workflow_errors import ExtractionWorkflowError
from docmind_ai.pii import SensitiveTokenizer
from docmind_ai.pii.models import SensitiveLeakError

TOKEN_PATTERN = re.compile(r"\[\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}\]\]")
TOKEN_PREFIX = "[[SENSITIVE:"  # noqa: S105


class ExtractionOutputValidator:
    def __init__(self, *, sensitive_tokenizer: SensitiveTokenizer | None = None) -> None:
        self._sensitive_tokenizer = sensitive_tokenizer or SensitiveTokenizer()

    def validate(
        self,
        request: AiExtractionRequest,
        output: ModelExtractionOutput,
    ) -> list[str]:
        self._validate_field_set(request, output)
        self._validate_data_consistency(output)
        self._validate_evidence(request, output)
        serialized = output.model_dump_json()
        self._validate_tokens(request, serialized)
        self._scan_for_plaintext_pii(request, output)
        return self._json_schema_errors(request, output)

    @staticmethod
    def _validate_field_set(request: AiExtractionRequest, output: ModelExtractionOutput) -> None:
        expected = [field.json_path for field in request.fields]
        actual = [field.path for field in output.fields]
        if len(actual) != len(set(actual)):
            raise ExtractionWorkflowError(
                "MODEL_FIELD_DUPLICATED", "模型返回了重复字段路径", retryable=False
            )
        if set(actual) != set(expected):
            raise ExtractionWorkflowError(
                "MODEL_FIELD_SET_MISMATCH", "模型返回字段集合与 Schema 不一致", retryable=False
            )

    @classmethod
    def _validate_data_consistency(cls, output: ModelExtractionOutput) -> None:
        for field in output.fields:
            resolved = cls._get_json_path(output.data, field.path)
            if resolved != field.value:
                raise ExtractionWorkflowError(
                    "MODEL_DATA_FIELD_MISMATCH",
                    "模型 data 与字段结果不一致",
                    retryable=False,
                )

    @staticmethod
    def _validate_evidence(request: AiExtractionRequest, output: ModelExtractionOutput) -> None:
        nodes = {node.node_id: node for node in request.document.nodes}
        for field in output.fields:
            all_evidence = list(field.evidence)
            all_evidence.extend(
                evidence for candidate in field.candidates for evidence in candidate.evidence
            )
            for evidence in all_evidence:
                ExtractionOutputValidator._validate_evidence_item(nodes, evidence)
            if field.candidates and not field.needs_review:
                raise ExtractionWorkflowError(
                    "MODEL_CANDIDATES_REVIEW_MISMATCH",
                    "多候选字段必须进入人工复核",
                    retryable=False,
                )

    @staticmethod
    def _validate_evidence_item(
        nodes: Mapping[str, TokenizedDocumentNode], evidence: ModelEvidence
    ) -> None:
        node = nodes.get(evidence.node_id)
        if node is None:
            raise ExtractionWorkflowError(
                "MODEL_EVIDENCE_NODE_UNKNOWN", "模型证据引用了未知节点", retryable=False
            )
        node_text = node.tokenized_text
        node_page = node.page_number
        if evidence.tokenized_text not in node_text or evidence.page_number != node_page:
            raise ExtractionWorkflowError(
                "MODEL_EVIDENCE_MISMATCH", "模型证据无法与原文节点对齐", retryable=False
            )

    @staticmethod
    def _validate_tokens(request: AiExtractionRequest, serialized_output: str) -> None:
        known_tokens = {
            match.group(0)
            for node in request.document.nodes
            for match in TOKEN_PATTERN.finditer(node.tokenized_text)
        }
        output_tokens = {match.group(0) for match in TOKEN_PATTERN.finditer(serialized_output)}
        unknown_tokens = output_tokens - known_tokens
        if unknown_tokens:
            raise ExtractionWorkflowError(
                "MODEL_UNKNOWN_SENSITIVE_TOKEN",
                "模型返回了未知敏感令牌",
                retryable=False,
            )
        without_valid_tokens = TOKEN_PATTERN.sub("[TOKEN]", serialized_output)
        if TOKEN_PREFIX in without_valid_tokens:
            raise ExtractionWorkflowError(
                "MODEL_MALFORMED_SENSITIVE_TOKEN",
                "模型修改了敏感令牌格式",
                retryable=False,
            )

    def _scan_for_plaintext_pii(
        self, request: AiExtractionRequest, output: ModelExtractionOutput
    ) -> None:
        content = [json.dumps(output.data, ensure_ascii=False, sort_keys=True)]
        for field in output.fields:
            content.extend(evidence.tokenized_text for evidence in field.evidence)
            for candidate in field.candidates:
                content.append(json.dumps(candidate.value, ensure_ascii=False, sort_keys=True))
                content.extend(evidence.tokenized_text for evidence in candidate.evidence)
        try:
            for value in content:
                self._sensitive_tokenizer.assert_no_plaintext_pii(
                    value,
                    country_codes=request.country_codes,
                )
        except SensitiveLeakError as exception:
            suffix = f"_{exception.data_types[0].upper()}" if exception.data_types else ""
            raise ExtractionWorkflowError(
                f"MODEL_PII_LEAK_DETECTED{suffix}",
                "模型输出包含未令牌化的敏感信息",
                retryable=False,
            ) from exception

    @staticmethod
    def _json_schema_errors(
        request: AiExtractionRequest, output: ModelExtractionOutput
    ) -> list[str]:
        schema = cast(dict[str, object], request.json_schema)
        try:
            Draft202012Validator.check_schema(schema)
        except SchemaError as exception:
            raise ExtractionWorkflowError(
                "REQUEST_JSON_SCHEMA_INVALID", "请求中的 JSON Schema 无效", retryable=False
            ) from exception
        validator = Draft202012Validator(schema)
        errors = sorted(validator.iter_errors(output.data), key=lambda error: list(error.path))
        return [
            f"{_safe_json_path(list(error.path))}: {error.validator or 'schema'} validation failed"
            for error in errors
        ]

    @staticmethod
    def _get_json_path(data: dict[str, JsonValue], path: str) -> JsonValue:
        current: JsonValue = data
        for part in path.removeprefix("$.").split("."):
            if not isinstance(current, dict) or part not in current:
                raise ExtractionWorkflowError(
                    "MODEL_DATA_PATH_MISSING", "模型 data 缺少字段路径", retryable=False
                )
            current = current[part]
        return current


def _safe_json_path(parts: list[object]) -> str:
    suffix = "".join(f"[{part}]" if isinstance(part, int) else f".{part}" for part in parts)
    return f"${suffix}"
