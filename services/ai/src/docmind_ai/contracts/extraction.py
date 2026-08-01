from typing import Literal
from uuid import UUID

from pydantic import Field, JsonValue

from docmind_ai.contracts.common import StrictModel
from docmind_ai.contracts.schema import SchemaFieldDefinition
from docmind_ai.contracts.sensitive import SensitiveRuleDefinition, SupportedCountryCode

DocumentNodeKind = Literal[
    "paragraph",
    "heading",
    "list_item",
    "table_cell",
    "header",
    "footer",
    "footnote",
    "endnote",
]


def _all_country_codes() -> list[SupportedCountryCode]:
    return ["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"]


class TokenizedDocumentNode(StrictModel):
    node_id: str = Field(min_length=1, max_length=255)
    kind: DocumentNodeKind
    page_number: int | None = Field(default=None, ge=1)
    tokenized_text: str = Field(max_length=1_000_000)
    metadata: dict[str, JsonValue] = Field(default_factory=dict)


class TokenizedDocument(StrictModel):
    source_version_id: UUID
    language: str | None = Field(default=None, max_length=35)
    nodes: list[TokenizedDocumentNode] = Field(max_length=100_000)


class AiExtractionRequest(StrictModel):
    request_id: UUID
    job_id: UUID
    workspace_id: UUID
    extraction_run_id: UUID
    source_version_id: UUID
    schema_version_id: UUID
    sensitive_rule_template_version_id: UUID | None
    country_codes: list[SupportedCountryCode] = Field(default_factory=_all_country_codes)
    fields: list[SchemaFieldDefinition] = Field(min_length=1, max_length=200)
    json_schema: dict[str, JsonValue]
    sensitive_rules: list[SensitiveRuleDefinition] = Field(default_factory=list, max_length=200)
    document: TokenizedDocument


class ModelEvidence(StrictModel):
    node_id: str = Field(min_length=1, max_length=255)
    page_number: int | None = Field(default=None, ge=1)
    tokenized_text: str = Field(max_length=10_000)


class ModelCandidateOutput(StrictModel):
    value: JsonValue
    confidence: float = Field(ge=0, le=1)
    evidence: list[ModelEvidence] = Field(min_length=1, max_length=100)


class ModelFieldOutput(StrictModel):
    path: str = Field(min_length=1, max_length=500)
    value: JsonValue
    confidence: float | None = Field(default=None, ge=0, le=1)
    evidence: list[ModelEvidence]
    candidates: list[ModelCandidateOutput] = Field(default_factory=list, max_length=20)
    needs_review: bool


class ModelExtractionOutput(StrictModel):
    data: dict[str, JsonValue]
    fields: list[ModelFieldOutput]


class ExtractionModelMetadata(StrictModel):
    provider: str = Field(min_length=1, max_length=100)
    model: str = Field(min_length=1, max_length=200)
    prompt_version: str = Field(min_length=1, max_length=100)
    input_tokens: int | None = Field(default=None, ge=0)
    output_tokens: int | None = Field(default=None, ge=0)


class AiExtractionResponse(StrictModel):
    request_id: UUID
    job_id: UUID
    extraction_run_id: UUID
    result: ModelExtractionOutput
    model: ExtractionModelMetadata
    validation_errors: list[str] = Field(default_factory=list)
