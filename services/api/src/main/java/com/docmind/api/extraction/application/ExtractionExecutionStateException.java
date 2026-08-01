package com.docmind.api.extraction.application;

public class ExtractionExecutionStateException extends RuntimeException {

  private final String failureCode;

  public ExtractionExecutionStateException(String failureCode) {
    super("Extraction execution state is invalid");
    this.failureCode = failureCode;
  }

  public String failureCode() {
    return failureCode;
  }
}
