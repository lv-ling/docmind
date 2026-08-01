package com.docmind.api.extraction.domain;

public enum ExtractionRunStatus {
  QUEUED,
  RUNNING,
  REVIEW_REQUIRED,
  APPROVED,
  FAILED,
  RETRYING;

  public String wireValue() {
    return name().toLowerCase();
  }
}
