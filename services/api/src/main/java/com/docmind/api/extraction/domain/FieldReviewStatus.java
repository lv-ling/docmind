package com.docmind.api.extraction.domain;

public enum FieldReviewStatus {
  PENDING,
  ACCEPTED,
  MODIFIED,
  REJECTED;

  public String wireValue() {
    return name().toLowerCase();
  }
}
