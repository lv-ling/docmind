package com.docmind.api.extraction.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ExtractionModelMetadataResponse(
    String provider,
    String model,
    String promptVersion,
    Integer inputTokens,
    Integer outputTokens) {}
