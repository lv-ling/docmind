package com.docmind.api.sensitive.domain;

public enum SensitiveRecognizerKind {
  PRESIDIO,
  REGEX,
  DICTIONARY,
  VALIDATOR;

  public String wireValue() {
    return name().toLowerCase();
  }
}
