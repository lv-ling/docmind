package com.docmind.api.shared.error;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApiErrorCategory {
  AUTHENTICATION("authentication"),
  AUTHORIZATION("authorization"),
  VALIDATION("validation"),
  RESOURCE("resource"),
  CONFLICT("conflict"),
  TASK("task"),
  DEPENDENCY("dependency"),
  RATE_LIMIT("rate_limit"),
  INTERNAL("internal");

  private final String wireValue;

  ApiErrorCategory(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String wireValue() {
    return wireValue;
  }
}
