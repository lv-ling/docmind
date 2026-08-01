package com.docmind.api.extraction.application;

public class ExtractionResultValidationException extends RuntimeException {

  private final String failureCode;

  public ExtractionResultValidationException(String failureCode) {
    super("AI extraction result failed contract validation");
    this.failureCode = failureCode;
  }

  public ExtractionResultValidationException(String failureCode, Throwable cause) {
    super("AI extraction result failed contract validation", cause);
    this.failureCode = failureCode;
  }

  public String failureCode() {
    return failureCode;
  }
}
