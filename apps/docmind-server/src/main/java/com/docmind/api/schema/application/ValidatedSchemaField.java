package com.docmind.api.schema.application;

import com.docmind.api.schema.api.SchemaFieldInput;
import com.docmind.api.schema.domain.FieldSensitivity;
import com.docmind.api.schema.domain.SchemaFieldDefaultKind;
import com.docmind.api.schema.domain.SchemaValueType;
import com.fasterxml.jackson.databind.JsonNode;

public record ValidatedSchemaField(
    SchemaFieldInput input,
    SchemaValueType valueType,
    SchemaValueType arrayItemType,
    SchemaFieldDefaultKind defaultKind,
    JsonNode defaultValue,
    FieldSensitivity sensitivity,
    JsonNode constraints,
    JsonNode examples,
    JsonNode display,
    JsonNode metadata) {}
