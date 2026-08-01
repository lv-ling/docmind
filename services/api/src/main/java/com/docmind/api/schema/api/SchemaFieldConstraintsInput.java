package com.docmind.api.schema.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SchemaFieldConstraintsInput(
    String format,
    String pattern,
    @NotNull List<@NotNull JsonNode> enumValues,
    Integer minLength,
    Integer maxLength,
    BigDecimal minimum,
    BigDecimal maximum) {}
