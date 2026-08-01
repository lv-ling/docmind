package com.docmind.api.template.domain;

public enum TemplateConversionStatus {
  QUEUED,
  RUNNING,
  READY,
  RETRYING,
  FAILED;

  public String wireValue() {
    return name().toLowerCase();
  }
}
