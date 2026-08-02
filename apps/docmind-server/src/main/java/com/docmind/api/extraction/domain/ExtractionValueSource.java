package com.docmind.api.extraction.domain;

public enum ExtractionValueSource {
  EXTRACTED,
  DEFAULT,
  MANUAL,
  NULL_VALUE;

  public String wireValue() {
    return this == NULL_VALUE ? "null" : name().toLowerCase();
  }
}
