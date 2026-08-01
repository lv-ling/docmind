package com.docmind.api.identity.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberStatus {
  ACTIVE("active"),
  SUSPENDED("suspended");

  private final String wireValue;

  MemberStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String wireValue() {
    return wireValue;
  }
}
