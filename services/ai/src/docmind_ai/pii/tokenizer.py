from __future__ import annotations

import re
from collections import defaultdict
from dataclasses import dataclass
from uuid import UUID, uuid5

from docmind_ai.contracts.sensitive import (
    SensitiveDataType,
    SensitiveDetection,
    SensitiveTextSpan,
    SensitiveTokenizationRequest,
    SensitiveTokenizationResponse,
    SensitiveTokenReference,
    SupportedCountryCode,
    TokenizedSensitiveTextNode,
)
from docmind_ai.pii.detector import SensitiveDetector
from docmind_ai.pii.models import DetectionCandidate, SensitiveLeakError

SENSITIVE_TOKEN_PATTERN = re.compile(r"\[\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}\]\]")


@dataclass(slots=True)
class _TokenAggregate:
    id: UUID
    token: str
    data_type: SensitiveDataType
    masked_preview: str
    occurrences: list[SensitiveTextSpan]


class SensitiveTokenizer:
    def __init__(self, detector: SensitiveDetector | None = None) -> None:
        self._detector = detector or SensitiveDetector()

    def tokenize(self, request: SensitiveTokenizationRequest) -> SensitiveTokenizationResponse:
        node_detections: list[tuple[int, list[DetectionCandidate]]] = []
        counters: defaultdict[SensitiveDataType, int] = defaultdict(int)
        token_by_value: dict[tuple[SensitiveDataType, str], str] = {}
        aggregates: dict[str, _TokenAggregate] = {}

        for node_index, node in enumerate(request.nodes):
            candidates = self._detector.detect(
                node.text,
                country_codes=request.country_codes,
                rules=request.rules,
            )
            node_detections.append((node_index, candidates))
            for candidate in candidates:
                value_key = (candidate.data_type, candidate.normalized_value)
                token = token_by_value.get(value_key)
                if token is None:
                    counters[candidate.data_type] += 1
                    token = self._token(candidate.data_type, counters[candidate.data_type])
                    token_by_value[value_key] = token
                    raw_value = node.text[candidate.start : candidate.end]
                    aggregates[token] = _TokenAggregate(
                        id=uuid5(
                            request.source_version_id,
                            f"{candidate.data_type}:{candidate.normalized_value}",
                        ),
                        token=token,
                        data_type=candidate.data_type,
                        masked_preview=self._mask(raw_value, candidate.data_type),
                        occurrences=[],
                    )
                aggregates[token].occurrences.append(
                    SensitiveTextSpan(
                        node_id=node.node_id,
                        start_offset=candidate.start,
                        end_offset=candidate.end,
                    )
                )

        tokenized_nodes: list[TokenizedSensitiveTextNode] = []
        serialized_detections: list[SensitiveDetection] = []
        for node_index, candidates in node_detections:
            node = request.nodes[node_index]
            tokenized_text = node.text
            replacements: list[tuple[DetectionCandidate, str]] = []
            for candidate in candidates:
                token = token_by_value[(candidate.data_type, candidate.normalized_value)]
                replacements.append((candidate, token))
                serialized_detections.append(
                    SensitiveDetection(
                        node_id=node.node_id,
                        start_offset=candidate.start,
                        end_offset=candidate.end,
                        data_type=candidate.data_type,
                        country_code=candidate.country_code,
                        confidence=candidate.score,
                        rule_key=candidate.rule_key,
                        token=token,
                    )
                )
            for candidate, token in reversed(replacements):
                tokenized_text = (
                    tokenized_text[: candidate.start] + token + tokenized_text[candidate.end :]
                )
            if len(tokenized_text) > 1_000_000:
                raise SensitiveLeakError("令牌化后的单节点文本超过安全限制")
            self.assert_no_plaintext_pii(
                tokenized_text,
                country_codes=request.country_codes,
            )
            tokenized_nodes.append(
                TokenizedSensitiveTextNode(
                    node_id=node.node_id,
                    kind=node.kind,
                    page_number=node.page_number,
                    tokenized_text=tokenized_text,
                    metadata=node.metadata,
                )
            )

        token_references = [
            SensitiveTokenReference(
                id=aggregate.id,
                source_version_id=request.source_version_id,
                token=aggregate.token,
                data_type=aggregate.data_type,
                masked_preview=aggregate.masked_preview,
                occurrences=aggregate.occurrences,
            )
            for aggregate in aggregates.values()
        ]
        return SensitiveTokenizationResponse(
            source_version_id=request.source_version_id,
            nodes=tokenized_nodes,
            tokens=token_references,
            detections=serialized_detections,
        )

    def assert_no_plaintext_pii(
        self,
        text: str,
        *,
        country_codes: list[SupportedCountryCode],
    ) -> None:
        scrubbed_tokens = SENSITIVE_TOKEN_PATTERN.sub("[TOKEN]", text)
        detected = self._detector.detect(scrubbed_tokens, country_codes=country_codes)
        if detected:
            data_types = tuple(sorted({candidate.data_type for candidate in detected}))
            raise SensitiveLeakError(
                "令牌化输出仍包含可识别敏感明文",
                data_types=data_types,
            )

    @staticmethod
    def _token(data_type: SensitiveDataType, sequence: int) -> str:
        return f"[[SENSITIVE:{data_type.upper()}:{sequence:02d}]]"

    @staticmethod
    def _mask(value: str, data_type: SensitiveDataType) -> str:
        if data_type == "email_address":
            return "***@***"
        compact = "".join(character for character in value if character.isalnum())
        if len(compact) <= 4:
            return "***"
        return f"{compact[:2]}***{compact[-2:]}"
