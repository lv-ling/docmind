package com.docmind.api.source.domain;

public enum UploadSessionStatus {
  PENDING,
  UPLOADING,
  COMPLETED,
  EXPIRED,
  ABORTED;

  public String wireValue() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
