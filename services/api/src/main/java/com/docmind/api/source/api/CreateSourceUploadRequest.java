package com.docmind.api.source.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateSourceUploadRequest(
    @NotBlank @Size(max = 200) String documentName,
    @NotBlank @Size(max = 255) String originalFileName,
    @NotBlank @Size(max = 255) String declaredMimeType,
    long sizeBytes) {}
