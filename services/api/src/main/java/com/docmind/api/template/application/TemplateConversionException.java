package com.docmind.api.template.application;

public class TemplateConversionException extends RuntimeException {
  private final String failureCode;
  private final boolean retryable;

  public TemplateConversionException(String failureCode, boolean retryable) {
    this(failureCode, retryable, null);
  }

  public TemplateConversionException(String failureCode, boolean retryable, Throwable cause) {
    super("Template conversion failed", cause);
    this.failureCode =
        failureCode != null && failureCode.matches("[A-Z][A-Z0-9_]{0,99}")
            ? failureCode
            : "TEMPLATE_CONVERSION_FAILED";
    this.retryable = retryable;
  }

  public String failureCode() { return failureCode; }
  public boolean retryable() { return retryable; }
}
