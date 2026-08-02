package com.docmind.api.shared.error;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ApiErrorResponse(
    ApiErrorCode code,
    ApiErrorCategory category,
    String message,
    Map<String, Object> details,
    List<ApiFieldErrorResponse> fieldErrors,
    String requestId,
    Instant timestamp) {}
