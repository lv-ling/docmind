package com.docmind.api.schema.api;

import com.docmind.api.schema.domain.SchemaTemplate;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SchemaTemplateResponse(
    UUID id,
    UUID workspaceId,
    String name,
    String description,
    UUID currentSchemaVersionId,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updatedBy) {

  public static SchemaTemplateResponse from(SchemaTemplate template) {
    return new SchemaTemplateResponse(
        template.id(),
        template.workspaceId(),
        template.name(),
        template.description(),
        template.currentSchemaVersionId(),
        template.createdAt(),
        template.createdBy(),
        template.updatedAt(),
        template.updatedBy());
  }
}
