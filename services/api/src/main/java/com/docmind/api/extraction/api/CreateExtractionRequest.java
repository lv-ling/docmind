package com.docmind.api.extraction.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateExtractionRequest(
    @NotNull UUID schemaVersionId, UUID sensitiveRuleTemplateVersionId) {}
