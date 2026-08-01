from __future__ import annotations

import hashlib
from dataclasses import dataclass, field
from typing import Literal
from uuid import UUID, uuid5

from pydantic import JsonValue

from docmind_ai.contracts.document import (
    ConversionWarning,
    DocumentInsets,
    DocumentLength,
    PageLayout,
    ParsedDocumentResource,
    ParsedTextNode,
)

PARSER_NAMESPACE = UUID("e86250d3-f0ee-44b8-9cc8-dde9ec5cb823")


class DocumentParsingError(Exception):
    def __init__(self, code: str, safe_message: str) -> None:
        super().__init__(safe_message)
        self.code = code
        self.safe_message = safe_message


@dataclass(slots=True)
class ParseContext:
    source_version_id: UUID
    language: str
    text_nodes: list[ParsedTextNode] = field(default_factory=list)
    resources: dict[UUID, ParsedDocumentResource] = field(default_factory=dict)
    warnings: list[ConversionWarning] = field(default_factory=list)

    def stable_uuid(self, locator: str) -> UUID:
        return uuid5(PARSER_NAMESPACE, f"{self.source_version_id}:{locator}")

    def node_id(self, locator: str) -> str:
        return f"node-{self.stable_uuid(locator)}"

    def add_warning(
        self,
        code: str,
        message: str,
        *,
        severity: str = "warning",
        node_id: str | None = None,
        page_number: int | None = None,
    ) -> None:
        normalized_severity = severity if severity in {"info", "warning", "error"} else "warning"
        self.warnings.append(
            ConversionWarning(
                code=code,
                severity=normalized_severity,  # type: ignore[arg-type]
                message=message,
                node_id=node_id,
                page_number=page_number,
            )
        )

    def add_resource(
        self,
        *,
        locator: str,
        media_type: str,
        filename: str,
        content_base64: str | None,
        raw_content: bytes | None,
        source_reference: dict[str, JsonValue],
    ) -> UUID:
        digest = hashlib.sha256(raw_content or locator.encode()).hexdigest()
        resource_id = self.stable_uuid(f"resource:{digest}")
        if resource_id not in self.resources:
            self.resources[resource_id] = ParsedDocumentResource(
                id=resource_id,
                media_type=media_type,
                filename=filename,
                sha256=digest,
                byte_length=len(raw_content or b""),
                content_base64=content_base64,
                source_reference=source_reference,
            )
        return resource_id


def points(value: float) -> DocumentLength:
    return DocumentLength(value=round(float(value), 3), unit="pt")


def default_page_layout(*, width: float = 595.276, height: float = 841.89) -> PageLayout:
    orientation: Literal["portrait", "landscape"] = "landscape" if width > height else "portrait"
    size: Literal["a4", "a3", "letter", "legal", "custom"] = "custom"
    dimensions = sorted((round(width), round(height)))
    if abs(dimensions[0] - 595) <= 3 and abs(dimensions[1] - 842) <= 3:
        size = "a4"
    elif abs(dimensions[0] - 612) <= 3 and abs(dimensions[1] - 792) <= 3:
        size = "letter"
    return PageLayout(
        size=size,
        orientation=orientation,
        width=points(width),
        height=points(height),
        margins=DocumentInsets(
            top=points(72),
            right=points(72),
            bottom=points(72),
            left=points(72),
        ),
        header_distance=points(36),
        footer_distance=points(36),
    )
