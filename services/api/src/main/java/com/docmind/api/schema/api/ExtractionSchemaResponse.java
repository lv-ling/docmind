package com.docmind.api.schema.api;

import com.docmind.api.schema.domain.ExtractionSchema;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExtractionSchemaResponse(
    UUID id,
    UUID workspaceId,
    String name,
    String description,
    UUID currentVersionId,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updatedBy) {

  public static ExtractionSchemaResponse from(ExtractionSchema schema) {
    return new ExtractionSchemaResponse(
        schema.id(),
        schema.workspaceId(),
        schema.name(),
        schema.description(),
        schema.currentVersionId(),
        schema.createdAt(),
        schema.createdBy(),
        schema.updatedAt(),
        schema.updatedBy());
  }
}
