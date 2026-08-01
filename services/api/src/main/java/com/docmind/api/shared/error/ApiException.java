package com.docmind.api.shared.error;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final ApiErrorCode code;
  private final ApiErrorCategory category;
  private final Map<String, Object> details;
  private final List<ApiFieldErrorResponse> fieldErrors;

  public ApiException(
      HttpStatus status, ApiErrorCode code, ApiErrorCategory category, String safeMessage) {
    this(status, code, category, safeMessage, Map.of(), List.of());
  }

  public ApiException(
      HttpStatus status,
      ApiErrorCode code,
      ApiErrorCategory category,
      String safeMessage,
      Map<String, Object> details,
      List<ApiFieldErrorResponse> fieldErrors) {
    super(safeMessage);
    this.status = status;
    this.code = code;
    this.category = category;
    this.details = Map.copyOf(details);
    this.fieldErrors = List.copyOf(fieldErrors);
  }

  public HttpStatus status() {
    return status;
  }

  public ApiErrorCode code() {
    return code;
  }

  public ApiErrorCategory category() {
    return category;
  }

  public Map<String, Object> details() {
    return details;
  }

  public List<ApiFieldErrorResponse> fieldErrors() {
    return fieldErrors;
  }
}
