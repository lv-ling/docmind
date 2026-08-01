package com.docmind.api.schema.domain;

public enum FieldSensitivity {
  NONE,
  LOW,
  MEDIUM,
  HIGH;

  public String wireValue() {
    return name().toLowerCase();
  }
}
