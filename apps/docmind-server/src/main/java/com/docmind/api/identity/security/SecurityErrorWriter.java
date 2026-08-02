package com.docmind.api.identity.security;

import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiErrorResponse;
import com.docmind.api.shared.web.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {

  private final ObjectMapper objectMapper;

  public SecurityErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void authenticationRequired(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer");
    write(
        request,
        response,
        HttpServletResponse.SC_UNAUTHORIZED,
        ApiErrorCode.AUTHENTICATION_REQUIRED,
        ApiErrorCategory.AUTHENTICATION,
        "需要登录后继续");
  }

  public void permissionDenied(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(
        request,
        response,
        HttpServletResponse.SC_FORBIDDEN,
        ApiErrorCode.PERMISSION_DENIED,
        ApiErrorCategory.AUTHORIZATION,
        "没有执行此操作的权限");
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      ApiErrorCode code,
      ApiErrorCategory category,
      String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiErrorResponse(
            code,
            category,
            message,
            Map.of(),
            List.of(),
            RequestContext.requestId(request),
            Instant.now()));
  }
}
