from typing import Annotated, Literal, Self
from uuid import UUID

from pydantic import Field, JsonValue, model_validator

from docmind_ai.contracts.common import StrictModel

SchemaValueType = Literal[
    "string", "number", "integer", "boolean", "date", "datetime", "object", "array"
]
FieldSensitivity = Literal["none", "low", "medium", "high"]


class NoSchemaFieldDefault(StrictModel):
    kind: Literal["none"]


class LiteralSchemaFieldDefault(StrictModel):
    kind: Literal["literal"]
    value: JsonValue


SchemaFieldDefault = Annotated[
    NoSchemaFieldDefault | LiteralSchemaFieldDefault, Field(discriminator="kind")
]


class SchemaFieldConstraints(StrictModel):
    format: str | None = None
    pattern: str | None = Field(default=None, max_length=2000)
    enum_values: list[JsonValue] = Field(default_factory=list, max_length=200)
    min_length: int | None = Field(default=None, ge=0)
    max_length: int | None = Field(default=None, ge=0)
    minimum: float | None = None
    maximum: float | None = None

    @model_validator(mode="after")
    def validate_bounds(self) -> Self:
        if (
            self.min_length is not None
            and self.max_length is not None
            and self.min_length > self.max_length
        ):
            raise ValueError("min_length cannot exceed max_length")
        if self.minimum is not None and self.maximum is not None and self.minimum > self.maximum:
            raise ValueError("minimum cannot exceed maximum")
        return self


class SchemaFieldDefinition(StrictModel):
    id: UUID
    key: str = Field(pattern=r"^[A-Za-z_][A-Za-z0-9_]*$", max_length=64)
    json_path: str = Field(
        pattern=r"^\$(?:\.[A-Za-z_][A-Za-z0-9_]*)+$", min_length=3, max_length=500
    )
    description: str = Field(max_length=2000)
    value_type: SchemaValueType
    array_item_type: SchemaValueType | None
    required: bool
    nullable: bool
    default: SchemaFieldDefault
    sensitivity: FieldSensitivity
    constraints: SchemaFieldConstraints
    examples: list[JsonValue] = Field(default_factory=list, max_length=20)
    extraction_hint: str | None = Field(default=None, max_length=2000)
    position: int = Field(ge=0)
