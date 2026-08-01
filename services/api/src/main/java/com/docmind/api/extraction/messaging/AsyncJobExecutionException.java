package com.docmind.api.extraction.messaging;

import java.util.regex.Pattern;

public class AsyncJobExecutionException extends RuntimeException {

  private static final Pattern SAFE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,99}");

  private final String failureCode;
  private final boolean retryable;

  public AsyncJobExecutionException(String failureCode, boolean retryable, Throwable cause) {
    super("Asynchronous job execution failed", cause);
    this.failureCode =
        failureCode != null && SAFE_CODE.matcher(failureCode).matches()
            ? failureCode
            : "JOB_EXECUTION_FAILED";
    this.retryable = retryable;
  }

  public String failureCode() {
    return failureCode;
  }

  public boolean retryable() {
    return retryable;
  }
}
