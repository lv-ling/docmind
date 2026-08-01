package com.docmind.api.source.domain;

public enum SourceFileType {
  DOC("doc", "application/msword"),
  DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
  PDF("pdf", "application/pdf");

  private final String wireValue;
  private final String mimeType;

  SourceFileType(String wireValue, String mimeType) {
    this.wireValue = wireValue;
    this.mimeType = mimeType;
  }

  public String wireValue() {
    return wireValue;
  }

  public String mimeType() {
    return mimeType;
  }
}
