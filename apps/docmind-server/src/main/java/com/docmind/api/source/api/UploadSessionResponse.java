package com.docmind.api.source.api;

import com.docmind.api.source.domain.SourceUploadSession;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UploadSessionResponse(
    UUID id,
    UUID sourceDocumentId,
    UUID sourceVersionId,
    String status,
    String uploadUrl,
    String uploadMethod,
    Map<String, String> requiredHeaders,
    long maxSizeBytes,
    Instant expiresAt,
    Instant createdAt) {

  public static UploadSessionResponse from(SourceUploadSession upload, String uploadUrl) {
    return new UploadSessionResponse(
        upload.id(),
        upload.sourceDocumentId(),
        upload.sourceVersionId(),
        upload.status().wireValue(),
        uploadUrl,
        "PUT",
        Map.of(),
        10_485_760,
        upload.expiresAt(),
        upload.createdAt());
  }
}
