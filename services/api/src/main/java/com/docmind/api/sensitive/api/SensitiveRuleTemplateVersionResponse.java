package com.docmind.api.sensitive.api;

import com.docmind.api.sensitive.domain.SensitiveRuleTemplateVersion;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SensitiveRuleTemplateVersionResponse(
    UUID id,
    UUID templateId,
    UUID workspaceId,
    int versionNumber,
    String status,
    List<SensitiveRuleResponse> rules,
    String changeSummary,
    Instant createdAt,
    UUID createdBy,
    Instant publishedAt) {

  public static SensitiveRuleTemplateVersionResponse from(
      SensitiveRuleTemplateVersion version, List<SensitiveRuleResponse> rules) {
    return new SensitiveRuleTemplateVersionResponse(
        version.id(),
        version.templateId(),
        version.workspaceId(),
        version.versionNumber(),
        version.status().wireValue(),
        List.copyOf(rules),
        version.changeSummary(),
        version.createdAt(),
        version.createdBy(),
        version.publishedAt());
  }
}
