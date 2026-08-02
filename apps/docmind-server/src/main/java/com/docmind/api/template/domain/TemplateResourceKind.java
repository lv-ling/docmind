package com.docmind.api.template.domain;

public enum TemplateResourceKind {
  IMAGE,
  STYLESHEET,
  FONT,
  ATTACHMENT;

  public String wireValue() {
    return name().toLowerCase();
  }
}
