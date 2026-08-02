package com.docmind.api.schema.domain;

public enum SchemaFieldDefaultKind {
  NONE,
  LITERAL;

  public String wireValue() {
    return name().toLowerCase();
  }
}
