package com.docmind.api.extraction.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ExtractionEvidenceViewResponse(
    Integer pageNumber,
    String nodeId,
    String displayText,
    boolean isMasked,
    Integer startOffset,
    Integer endOffset) {}
