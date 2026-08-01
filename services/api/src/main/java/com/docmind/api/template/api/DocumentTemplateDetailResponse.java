package com.docmind.api.template.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DocumentTemplateDetailResponse(
    DocumentTemplateResponse template,
    DocumentTemplateVersionResponse currentVersion,
    List<DocumentTemplateVersionResponse> versions) {}
