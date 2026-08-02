package com.docmind.api.schema.domain;

public enum SchemaVersionStatus {
  DRAFT,
  PUBLISHED,
  SUPERSEDED;

  public String wireValue() {
    return name().toLowerCase();
  }
}
