package com.docmind.api.source.api;

import com.docmind.api.source.domain.SourceDocument;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SourceDocumentResponse(
    UUID id,
    UUID workspaceId,
    String name,
    UUID currentVersionId,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updatedBy) {

  public static SourceDocumentResponse from(SourceDocument source) {
    return new SourceDocumentResponse(
        source.id(),
        source.workspaceId(),
        source.name(),
        source.currentVersionId(),
        source.createdAt(),
        source.createdBy(),
        source.updatedAt(),
        source.updatedBy());
  }
}
