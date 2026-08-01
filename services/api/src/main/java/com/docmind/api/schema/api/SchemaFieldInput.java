package com.docmind.api.schema.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SchemaFieldInput(
    @NotBlank @Size(max = 64) String key,
    @NotBlank @Size(max = 500) String jsonPath,
    @NotNull @Size(max = 2000) String description,
    @NotBlank String valueType,
    String arrayItemType,
    boolean required,
    boolean nullable,
    @JsonProperty("default") @NotNull @Valid SchemaFieldDefaultInput defaultValue,
    @NotBlank String sensitivity,
    @NotNull @Valid SchemaFieldConstraintsInput constraints,
    @NotNull @Size(max = 20) List<@NotNull JsonNode> examples,
    @Size(max = 2000) String extractionHint,
    @NotNull @Valid SchemaFieldDisplayInput display,
    @NotNull JsonNode metadata,
    @Min(0) int position) {}
