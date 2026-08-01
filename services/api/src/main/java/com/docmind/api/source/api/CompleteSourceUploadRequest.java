package com.docmind.api.source.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CompleteSourceUploadRequest(
    @Min(1) @Max(10_485_760) long sizeBytes,
    @NotBlank @Size(max = 255) String detectedMimeType,
    @NotBlank @Pattern(regexp = "^[a-fA-F0-9]{64}$") String sha256,
    @NotBlank @Size(max = 255) String objectEtag) {}
