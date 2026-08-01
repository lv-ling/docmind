package com.docmind.api.extraction.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AcceptedExtractionJobResponse(UUID jobId, UUID extractionId, UUID requestId) {}
