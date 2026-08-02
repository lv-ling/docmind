package com.docmind.api.schema.api;

import com.docmind.api.schema.domain.ExtractionSchemaVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SchemaVersionResponse(
    UUID id,
    UUID schemaId,
    UUID workspaceId,
    int versionNumber,
    String status,
    List<SchemaFieldResponse> fields,
    JsonNode jsonSchema,
    String changeSummary,
    Instant createdAt,
    UUID createdBy,
    Instant publishedAt) {

  public static SchemaVersionResponse from(
      ExtractionSchemaVersion version, List<SchemaFieldResponse> fields) {
    return new SchemaVersionResponse(
        version.id(),
        version.schemaId(),
        version.workspaceId(),
        version.versionNumber(),
        version.status().wireValue(),
        List.copyOf(fields),
        version.jsonSchema(),
        version.changeSummary(),
        version.createdAt(),
        version.createdBy(),
        version.publishedAt());
  }
}
