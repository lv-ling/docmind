package com.docmind.api.template.api;

import com.docmind.api.template.domain.DocumentTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DocumentTemplateResponse(
    UUID id,
    UUID workspaceId,
    UUID sourceDocumentId,
    UUID sourceVersionId,
    UUID conversionJobId,
    String name,
    UUID currentVersionId,
    String conversionStatus,
    String failureCode,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updatedBy) {
  public static DocumentTemplateResponse from(DocumentTemplate template) {
    return new DocumentTemplateResponse(
        template.id(),
        template.workspaceId(),
        template.sourceDocumentId(),
        template.sourceVersionId(),
        template.conversionJobId(),
        template.name(),
        template.currentVersionId(),
        template.conversionStatus().wireValue(),
        template.failureCode(),
        template.createdAt(),
        template.createdBy(),
        template.updatedAt(),
        template.updatedBy());
  }
}
