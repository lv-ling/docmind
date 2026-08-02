package com.docmind.api.template.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateTemplateVersionRequest(
    @NotNull UUID baseVersionId,
    @NotNull JsonNode documentModel,
    @jakarta.validation.constraints.NotBlank @Size(max = 1000) String changeSummary) {}
