package com.docmind.api.shared.error;

import com.docmind.api.shared.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiErrorResponse> handleApiException(
      ApiException exception, HttpServletRequest request) {
    return response(
        exception.status(),
        exception.code(),
        exception.category(),
        exception.getMessage(),
        exception.details(),
        exception.fieldErrors(),
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiFieldErrorResponse> fieldErrors = new ArrayList<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      fieldErrors.add(
          new ApiFieldErrorResponse(
              error.getField(),
              error.getCode() == null ? "invalid" : error.getCode(),
              error.getDefaultMessage() == null ? "字段值无效" : error.getDefaultMessage()));
    }
    exception
        .getBindingResult()
        .getGlobalErrors()
        .forEach(
            error ->
                fieldErrors.add(
                    new ApiFieldErrorResponse(
                        "$",
                        error.getCode() == null ? "invalid" : error.getCode(),
                        error.getDefaultMessage() == null ? "请求无效" : error.getDefaultMessage())));

    return validationResponse("请求字段校验失败", fieldErrors, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<ApiFieldErrorResponse> fieldErrors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new ApiFieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        violation.getMessage()))
            .toList();
    return validationResponse("请求参数校验失败", fieldErrors, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return validationResponse(
        "请求体不是有效的 JSON", List.of(new ApiFieldErrorResponse("$", "invalid_json", "JSON 格式无效")), request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiErrorResponse> handleNoResource(
      NoResourceFoundException exception, HttpServletRequest request) {
    return response(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        "请求的资源不存在",
        Map.of(),
        List.of(),
        request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    // Exception messages may contain submitted document data; log only the class here.
    log.error("api_request_failed exception_type={}", exception.getClass().getName());
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ApiErrorCode.INTERNAL_ERROR,
        ApiErrorCategory.INTERNAL,
        "服务暂时无法处理该请求",
        Map.of(),
        List.of(),
        request);
  }

  private ResponseEntity<ApiErrorResponse> validationResponse(
      String message, List<ApiFieldErrorResponse> fieldErrors, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_FAILED,
        ApiErrorCategory.VALIDATION,
        message,
        Map.of(),
        fieldErrors,
        request);
  }

  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status,
      ApiErrorCode code,
      ApiErrorCategory category,
      String message,
      Map<String, Object> details,
      List<ApiFieldErrorResponse> fieldErrors,
      HttpServletRequest request) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            code,
            category,
            message,
            details,
            fieldErrors,
            RequestContext.requestId(request),
            Instant.now());
    return ResponseEntity.status(status).body(body);
  }
}
