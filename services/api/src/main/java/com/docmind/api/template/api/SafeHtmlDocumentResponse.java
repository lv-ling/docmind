package com.docmind.api.template.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SafeHtmlDocumentResponse(
    String html, String css, String sanitizationPolicyVersion) {}
