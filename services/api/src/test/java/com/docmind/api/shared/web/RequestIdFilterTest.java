package com.docmind.api.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void preservesAValidCallerRequestIdDuringTheRequest() throws Exception {
    String requestId = UUID.randomUUID().toString();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sources");
    request.addHeader(RequestContext.REQUEST_ID_HEADER, requestId);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> requestIdSeenByHandler = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (servletRequest, servletResponse) ->
            requestIdSeenByHandler.set(MDC.get(RequestContext.REQUEST_ID_MDC_KEY)));

    assertThat(request.getAttribute(RequestContext.REQUEST_ID_ATTRIBUTE)).isEqualTo(requestId);
    assertThat(response.getHeader(RequestContext.REQUEST_ID_HEADER)).isEqualTo(requestId);
    assertThat(requestIdSeenByHandler).hasValue(requestId);
    assertThat(MDC.get(RequestContext.REQUEST_ID_MDC_KEY)).isNull();
  }

  @Test
  void replacesAnUntrustedRequestId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sources");
    request.addHeader(RequestContext.REQUEST_ID_HEADER, "line-one\nforged-log-line");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

    String generated = response.getHeader(RequestContext.REQUEST_ID_HEADER);
    assertThat(generated).isNotEqualTo("line-one\nforged-log-line");
    assertThatCode(() -> UUID.fromString(generated)).doesNotThrowAnyException();
  }
}
