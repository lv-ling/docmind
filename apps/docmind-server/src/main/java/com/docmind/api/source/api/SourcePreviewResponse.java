package com.docmind.api.source.api;

import com.docmind.api.source.domain.SourcePreview;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SourcePreviewResponse(
    UUID id,
    UUID sourceVersionId,
    String status,
    String format,
    Integer pageCount,
    String failureCode,
    Instant createdAt,
    Instant completedAt) {

  public static SourcePreviewResponse from(SourcePreview preview) {
    return new SourcePreviewResponse(
        preview.id(),
        preview.sourceVersionId(),
        preview.status().wireValue(),
        preview.format().wireValue(),
        preview.pageCount(),
        preview.failureCode(),
        preview.createdAt(),
        preview.completedAt());
  }
}
