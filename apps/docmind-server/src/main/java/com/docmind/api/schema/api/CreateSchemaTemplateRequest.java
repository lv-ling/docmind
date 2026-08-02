package com.docmind.api.schema.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateSchemaTemplateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Size(max = 2000) String description,
    @NotNull UUID schemaVersionId) {}
