package com.docmind.api.sensitive.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SensitiveRuleInput(
    @NotBlank @Size(max = 64) String key,
    @NotBlank @Size(max = 100) String name,
    @NotNull @Size(max = 2000) String description,
    @NotBlank String dataType,
    @NotBlank String recognizerKind,
    @NotNull @Size(max = 50) List<@NotBlank @Size(max = 35) String> locales,
    @NotNull @Size(max = 50) List<@NotBlank @Size(min = 2, max = 2) String> countryCodes,
    @Size(max = 2000) String regexPattern,
    String regexDialect,
    @NotNull @Size(max = 10000) List<@NotBlank @Size(max = 256) String> dictionaryTerms,
    @Size(max = 100) String validatorName,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidenceThreshold,
    @Min(-1000) @Max(1000) int priority,
    boolean enabled) {}
