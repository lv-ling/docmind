package com.docmind.api.schema.api;

import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SchemaFieldResponse(
    UUID id,
    String key,
    String jsonPath,
    String description,
    String valueType,
    String arrayItemType,
    boolean required,
    boolean nullable,
    @JsonProperty("default") Map<String, Object> defaultValue,
    String sensitivity,
    JsonNode constraints,
    JsonNode examples,
    String extractionHint,
    JsonNode display,
    JsonNode metadata,
    int position) {

  public static SchemaFieldResponse from(ExtractionSchemaField field) {
    Map<String, Object> defaultValue = new LinkedHashMap<>();
    defaultValue.put("kind", field.defaultKind().wireValue());
    if (field.defaultKind().wireValue().equals("literal")) {
      defaultValue.put("value", field.defaultValue());
    }
    return new SchemaFieldResponse(
        field.id(),
        field.key(),
        field.jsonPath(),
        field.description(),
        field.valueType().wireValue(),
        field.arrayItemType() == null ? null : field.arrayItemType().wireValue(),
        field.required(),
        field.nullable(),
        defaultValue,
        field.sensitivity().wireValue(),
        field.constraints(),
        field.examples(),
        field.extractionHint(),
        field.display(),
        field.metadata(),
        field.position());
  }
}
