package com.docmind.api.source.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SourcePreviewAccessResponse(
    SourcePreviewResponse preview, String viewUrl, String originalContentUrl) {}
