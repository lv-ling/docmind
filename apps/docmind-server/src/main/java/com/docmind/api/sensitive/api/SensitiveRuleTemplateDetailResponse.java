package com.docmind.api.sensitive.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SensitiveRuleTemplateDetailResponse(
    SensitiveRuleTemplateResponse template,
    SensitiveRuleTemplateVersionResponse currentVersion,
    List<SensitiveRuleTemplateVersionResponse> versions) {}
