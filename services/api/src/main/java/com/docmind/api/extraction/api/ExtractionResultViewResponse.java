package com.docmind.api.extraction.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExtractionResultViewResponse(
    JsonNode data,
    boolean containsMaskedValues,
    List<ExtractionFieldResultViewResponse> fields,
    ExtractionModelMetadataResponse model,
    List<String> validationErrors) {}
