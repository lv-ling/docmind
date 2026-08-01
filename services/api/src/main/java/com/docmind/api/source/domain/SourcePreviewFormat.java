package com.docmind.api.source.domain;

public enum SourcePreviewFormat {
  PDF,
  PAGE_IMAGES;

  public String wireValue() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
