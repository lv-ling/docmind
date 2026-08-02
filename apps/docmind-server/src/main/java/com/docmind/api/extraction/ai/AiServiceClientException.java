package com.docmind.api.extraction.ai;

import java.util.regex.Pattern;

public class AiServiceClientException extends RuntimeException {

  private static final Pattern SAFE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,99}");

  private final String failureCode;
  private final boolean retryable;

  public AiServiceClientException(String failureCode, boolean retryable, Throwable cause) {
    super("AI service request failed", cause);
    this.failureCode =
        failureCode != null && SAFE_CODE.matcher(failureCode).matches()
            ? failureCode
            : "AI_SERVICE_FAILURE";
    this.retryable = retryable;
  }

  public String failureCode() {
    return failureCode;
  }

  public boolean retryable() {
    return retryable;
  }
}
