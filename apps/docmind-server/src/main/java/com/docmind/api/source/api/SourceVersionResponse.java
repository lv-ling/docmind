package com.docmind.api.source.api;

import com.docmind.api.source.domain.SourceVersion;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SourceVersionResponse(
    UUID id,
    UUID sourceDocumentId,
    UUID workspaceId,
    int versionNumber,
    String status,
    String originalFileName,
    String fileType,
    String declaredMimeType,
    long expectedSizeBytes,
    SourceFileMetadataResponse file,
    String failureCode,
    Instant createdAt) {

  public static SourceVersionResponse from(SourceVersion version) {
    return new SourceVersionResponse(
        version.id(),
        version.sourceDocumentId(),
        version.workspaceId(),
        version.versionNumber(),
        version.status().wireValue(),
        version.originalFileName(),
        version.fileType().wireValue(),
        version.declaredMimeType(),
        version.expectedSizeBytes(),
        SourceFileMetadataResponse.from(version),
        version.failureCode(),
        version.createdAt());
  }
}
