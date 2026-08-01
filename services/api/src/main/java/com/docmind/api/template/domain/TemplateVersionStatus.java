package com.docmind.api.template.domain;

public enum TemplateVersionStatus {
  GENERATED,
  CHECKING,
  PUBLISHED,
  SUPERSEDED;

  public String wireValue() {
    return name().toLowerCase();
  }
}
