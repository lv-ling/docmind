from __future__ import annotations

from uuid import UUID, uuid4

from pydantic import JsonValue

from docmind_ai.contracts.extraction import (
    AiExtractionRequest,
    TokenizedDocument,
    TokenizedDocumentNode,
)
from docmind_ai.contracts.schema import (
    LiteralSchemaFieldDefault,
    NoSchemaFieldDefault,
    SchemaFieldConstraints,
    SchemaFieldDefinition,
)


def schema_field(
    *,
    key: str,
    json_path: str | None = None,
    description: str | None = None,
    value_type: str = "string",
    required: bool = True,
    nullable: bool = False,
    default: JsonValue | object = None,
    has_default: bool = False,
    position: int = 0,
) -> SchemaFieldDefinition:
    field_default = (
        LiteralSchemaFieldDefault(kind="literal", value=default)
        if has_default
        else NoSchemaFieldDefault(kind="none")
    )
    return SchemaFieldDefinition(
        id=uuid4(),
        key=key,
        json_path=json_path or f"$.{key}",
        description=description or key,
        value_type=value_type,
        array_item_type=None,
        required=required,
        nullable=nullable,
        default=field_default,
        sensitivity="none",
        constraints=SchemaFieldConstraints(),
        examples=[],
        extraction_hint=None,
        position=position,
    )


def extraction_request(
    *,
    fields: list[SchemaFieldDefinition],
    texts: list[str],
    json_schema: dict[str, JsonValue],
    source_version_id: UUID | None = None,
) -> AiExtractionRequest:
    resolved_source_version_id = source_version_id or uuid4()
    return AiExtractionRequest(
        request_id=uuid4(),
        job_id=uuid4(),
        workspace_id=uuid4(),
        extraction_run_id=uuid4(),
        source_version_id=resolved_source_version_id,
        schema_version_id=uuid4(),
        sensitive_rule_template_version_id=None,
        country_codes=["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"],
        fields=fields,
        json_schema=json_schema,
        sensitive_rules=[],
        document=TokenizedDocument(
            source_version_id=resolved_source_version_id,
            language="zh-CN",
            nodes=[
                TokenizedDocumentNode(
                    node_id=f"node-{index}",
                    kind="paragraph",
                    page_number=index + 1,
                    tokenized_text=text,
                    metadata={},
                )
                for index, text in enumerate(texts)
            ],
        ),
    )
