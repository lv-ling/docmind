package com.docmind.api.sensitive.domain;

public enum SensitiveRuleTemplateVersionStatus {
  DRAFT,
  PUBLISHED,
  SUPERSEDED;

  public String wireValue() {
    return name().toLowerCase();
  }
}
