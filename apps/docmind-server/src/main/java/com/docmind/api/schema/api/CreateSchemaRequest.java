package com.docmind.api.schema.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateSchemaRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Size(max = 2000) String description,
    @NotNull @Size(min = 1, max = 200) List<@Valid SchemaFieldInput> fields) {}
