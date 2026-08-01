package com.docmind.api.identity.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UserStatus {
  ACTIVE("active"),
  DISABLED("disabled");

  private final String wireValue;

  UserStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String wireValue() {
    return wireValue;
  }
}
