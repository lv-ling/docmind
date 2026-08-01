package com.docmind.api.sensitive.api;

import com.docmind.api.sensitive.domain.SensitiveRuleTemplate;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SensitiveRuleTemplateResponse(
    UUID id,
    UUID workspaceId,
    String name,
    String description,
    UUID currentVersionId,
    Instant createdAt,
    UUID createdBy,
    Instant updatedAt,
    UUID updatedBy) {

  public static SensitiveRuleTemplateResponse from(SensitiveRuleTemplate template) {
    return new SensitiveRuleTemplateResponse(
        template.id(),
        template.workspaceId(),
        template.name(),
        template.description(),
        template.currentVersionId(),
        template.createdAt(),
        template.createdBy(),
        template.updatedAt(),
        template.updatedBy());
  }
}
