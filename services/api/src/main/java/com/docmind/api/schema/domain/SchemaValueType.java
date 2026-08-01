package com.docmind.api.schema.domain;

public enum SchemaValueType {
  STRING,
  NUMBER,
  INTEGER,
  BOOLEAN,
  DATE,
  DATETIME,
  OBJECT,
  ARRAY;

  public String wireValue() {
    return name().toLowerCase();
  }
}
