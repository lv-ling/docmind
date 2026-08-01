package com.docmind.api.sensitive.api;

import com.docmind.api.sensitive.domain.SensitiveRule;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SensitiveRuleResponse(
    UUID id,
    String key,
    String name,
    String description,
    String dataType,
    String recognizerKind,
    List<String> locales,
    List<String> countryCodes,
    String regexPattern,
    String regexDialect,
    List<String> dictionaryTerms,
    String validatorName,
    BigDecimal confidenceThreshold,
    int priority,
    boolean enabled) {

  public static SensitiveRuleResponse from(SensitiveRule rule) {
    return new SensitiveRuleResponse(
        rule.id(),
        rule.key(),
        rule.name(),
        rule.description(),
        rule.dataType().wireValue(),
        rule.recognizerKind().wireValue(),
        rule.locales(),
        rule.countryCodes(),
        rule.regexPattern(),
        rule.regexDialect(),
        rule.dictionaryTerms(),
        rule.validatorName(),
        rule.confidenceThreshold(),
        rule.priority(),
        rule.enabled());
  }
}
