package com.docmind.api.source.domain;

public enum SourceVersionStatus {
  UPLOADING,
  UPLOADED,
  PROCESSING,
  READY,
  FAILED;

  public String wireValue() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
