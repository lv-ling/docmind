package com.docmind.api.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = normalizeOrGenerate(request.getHeader(RequestContext.REQUEST_ID_HEADER));
    long startedAt = System.nanoTime();

    request.setAttribute(RequestContext.REQUEST_ID_ATTRIBUTE, requestId);
    response.setHeader(RequestContext.REQUEST_ID_HEADER, requestId);
    MDC.put(RequestContext.REQUEST_ID_MDC_KEY, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.info(
          "api_request_completed method={} path={} status={} duration_ms={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
      MDC.remove(RequestContext.REQUEST_ID_MDC_KEY);
    }
  }

  static String normalizeOrGenerate(String candidate) {
    if (candidate != null) {
      try {
        return UUID.fromString(candidate).toString();
      } catch (IllegalArgumentException ignored) {
        // Untrusted request IDs must not enter logs or downstream calls.
      }
    }
    return UUID.randomUUID().toString();
  }
}
