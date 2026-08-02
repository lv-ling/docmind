package com.docmind.api.identity.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkspaceRole {
  OWNER("owner"),
  ADMIN("admin"),
  EDITOR("editor"),
  REVIEWER("reviewer"),
  VIEWER("viewer");

  private final String wireValue;

  WorkspaceRole(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String wireValue() {
    return wireValue;
  }
}
