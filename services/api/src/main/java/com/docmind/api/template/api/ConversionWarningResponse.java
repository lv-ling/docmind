package com.docmind.api.template.api;

import com.docmind.api.template.domain.DocumentConversionWarning;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ConversionWarningResponse(
    UUID id,
    String severity,
    String code,
    String message,
    String sourceNodeId,
    Integer pageNumber,
    String fallback,
    boolean blocking) {
  public static ConversionWarningResponse from(DocumentConversionWarning warning) {
    return new ConversionWarningResponse(
        warning.id(),
        warning.severity().wireValue(),
        warning.code(),
        warning.message(),
        warning.sourceNodeId(),
        warning.pageNumber(),
        warning.fallback(),
        warning.blocking());
  }
}
