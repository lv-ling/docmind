package com.docmind.api.extraction.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExtractionCandidateViewResponse(
    JsonNode displayValue,
    BigDecimal confidence,
    List<ExtractionEvidenceViewResponse> evidence) {}
