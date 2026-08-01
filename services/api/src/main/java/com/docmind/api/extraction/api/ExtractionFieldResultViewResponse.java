package com.docmind.api.extraction.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ExtractionFieldResultViewResponse(
    UUID id,
    String jsonPath,
    JsonNode displayValue,
    String valueSource,
    String missingReason,
    BigDecimal confidence,
    List<ExtractionEvidenceViewResponse> evidence,
    List<ExtractionCandidateViewResponse> candidates,
    boolean needsReview,
    String reviewStatus) {}
