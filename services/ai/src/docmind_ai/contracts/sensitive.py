from typing import Literal
from uuid import UUID

from pydantic import Field, JsonValue

from docmind_ai.contracts.common import StrictModel

SensitiveDataType = Literal[
    "china_national_id",
    "identity_document",
    "passport",
    "phone_number",
    "email_address",
    "credit_card",
    "bank_account",
    "ip_address",
    "person_name",
    "location",
    "custom",
]
SupportedCountryCode = Literal["CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL"]
RecognizerKind = Literal["presidio", "regex", "dictionary", "validator"]
ValidatorName = Literal[
    "cn_resident_identity",
    "e164_phone",
    "email",
    "luhn",
    "iban",
    "ip_address",
    "passport_document",
]
SensitiveNodeKind = Literal[
    "paragraph",
    "heading",
    "list_item",
    "table_cell",
    "header",
    "footer",
    "footnote",
    "endnote",
]


class SensitiveRuleDefinition(StrictModel):
    id: UUID
    key: str = Field(min_length=1, max_length=64)
    name: str = Field(min_length=1, max_length=200)
    description: str = Field(max_length=1000)
    data_type: SensitiveDataType
    recognizer_kind: RecognizerKind
    locales: list[str] = Field(default_factory=list, max_length=50)
    country_codes: list[SupportedCountryCode] = Field(default_factory=list, max_length=50)
    regex_pattern: str | None = Field(default=None, max_length=2000)
    regex_dialect: Literal["re2"] | None = None
    dictionary_terms: list[str] = Field(default_factory=list, max_length=10_000)
    validator_name: ValidatorName | None = None
    confidence_threshold: float = Field(ge=0, le=1)
    priority: int = Field(ge=-1000, le=1000)
    enabled: bool


class SensitiveTextNode(StrictModel):
    node_id: str = Field(min_length=1, max_length=255)
    kind: SensitiveNodeKind
    page_number: int | None = Field(default=None, ge=1)
    text: str = Field(max_length=1_000_000)
    metadata: dict[str, JsonValue] = Field(default_factory=dict)


class SensitiveTokenizationRequest(StrictModel):
    source_version_id: UUID
    language: str = Field(min_length=1, max_length=35)
    country_codes: list[SupportedCountryCode] = Field(min_length=1, max_length=9)
    rules: list[SensitiveRuleDefinition] = Field(default_factory=list, max_length=200)
    nodes: list[SensitiveTextNode] = Field(max_length=100_000)


class SensitiveTextSpan(StrictModel):
    node_id: str = Field(min_length=1, max_length=255)
    start_offset: int = Field(ge=0)
    end_offset: int = Field(gt=0)


class SensitiveTokenReference(StrictModel):
    id: UUID
    source_version_id: UUID
    token: str = Field(pattern=r"^\[\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}\]\]$")
    data_type: SensitiveDataType
    masked_preview: str = Field(min_length=1, max_length=100)
    occurrences: list[SensitiveTextSpan] = Field(min_length=1)


class SensitiveDetection(StrictModel):
    node_id: str = Field(min_length=1, max_length=255)
    start_offset: int = Field(ge=0)
    end_offset: int = Field(gt=0)
    data_type: SensitiveDataType
    country_code: SupportedCountryCode | None = None
    confidence: float = Field(ge=0, le=1)
    rule_key: str = Field(min_length=1, max_length=100)
    token: str = Field(pattern=r"^\[\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}\]\]$")


class TokenizedSensitiveTextNode(StrictModel):
    node_id: str = Field(min_length=1, max_length=255)
    kind: SensitiveNodeKind
    page_number: int | None = Field(default=None, ge=1)
    tokenized_text: str = Field(max_length=1_000_000)
    metadata: dict[str, JsonValue] = Field(default_factory=dict)


class SensitiveTokenizationResponse(StrictModel):
    source_version_id: UUID
    nodes: list[TokenizedSensitiveTextNode] = Field(max_length=100_000)
    tokens: list[SensitiveTokenReference] = Field(max_length=100_000)
    detections: list[SensitiveDetection] = Field(max_length=100_000)
