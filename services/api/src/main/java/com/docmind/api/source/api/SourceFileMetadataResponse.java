package com.docmind.api.source.api;

import com.docmind.api.source.domain.SourceVersion;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SourceFileMetadataResponse(
    String originalFileName, String fileType, String mimeType, long sizeBytes, String sha256) {

  public static SourceFileMetadataResponse from(SourceVersion version) {
    if (version.sha256() == null || version.sizeBytes() == null || version.mimeType() == null) {
      return null;
    }
    return new SourceFileMetadataResponse(
        version.originalFileName(),
        version.fileType().wireValue(),
        version.mimeType(),
        version.sizeBytes(),
        version.sha256());
  }
}
