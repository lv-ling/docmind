package com.docmind.api.extraction.api;

import com.docmind.api.extraction.domain.ExtractionRun;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ExtractionRunResponse(
    UUID id,
    UUID jobId,
    UUID workspaceId,
    UUID sourceVersionId,
    UUID schemaVersionId,
    UUID sensitiveRuleTemplateVersionId,
    String status,
    ExtractionResultViewResponse result,
    String failureCode,
    Instant createdAt,
    Instant completedAt) {

  public static ExtractionRunResponse from(
      ExtractionRun run, ExtractionResultViewResponse safeResult) {
    return new ExtractionRunResponse(
        run.id(),
        run.jobId(),
        run.workspaceId(),
        run.sourceVersionId(),
        run.schemaVersionId(),
        run.sensitiveRuleTemplateVersionId(),
        run.status().wireValue(),
        safeResult,
        run.failureCode(),
        run.createdAt(),
        run.completedAt());
  }
}
