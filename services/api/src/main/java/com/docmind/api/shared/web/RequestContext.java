package com.docmind.api.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public final class RequestContext {

  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String REQUEST_ID_ATTRIBUTE = RequestContext.class.getName() + ".requestId";
  public static final String REQUEST_ID_MDC_KEY = "request_id";

  private RequestContext() {}

  public static String requestId(HttpServletRequest request) {
    Object value = request.getAttribute(REQUEST_ID_ATTRIBUTE);
    return value instanceof String requestId ? requestId : UUID.randomUUID().toString();
  }
}
