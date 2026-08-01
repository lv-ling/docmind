package com.docmind.api.extraction.domain;

public enum ExtractionMissingReason {
  NOT_FOUND,
  INSUFFICIENT_EVIDENCE,
  AMBIGUOUS,
  INVALID_MODEL_OUTPUT,
  SENSITIVE_TOKEN_MISSING;

  public String wireValue() {
    return name().toLowerCase();
  }
}
