package com.docmind.api.source.domain;

public enum SourcePreviewStatus {
  QUEUED,
  PROCESSING,
  READY,
  FAILED;

  public String wireValue() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
