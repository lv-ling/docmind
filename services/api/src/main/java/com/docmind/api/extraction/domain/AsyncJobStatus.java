package com.docmind.api.extraction.domain;

public enum AsyncJobStatus {
  QUEUED,
  RUNNING,
  RETRYING,
  SUCCEEDED,
  FAILED;

  public String wireValue() {
    return name().toLowerCase();
  }
}
