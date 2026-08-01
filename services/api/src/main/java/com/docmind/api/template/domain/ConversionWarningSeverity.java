package com.docmind.api.template.domain;

public enum ConversionWarningSeverity {
  INFO,
  WARNING,
  ERROR;

  public String wireValue() {
    return name().toLowerCase();
  }
}
