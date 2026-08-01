package com.docmind.api.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "source_upload_session",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_source_upload_creation_key",
            columnNames = {"workspace_id", "created_by", "creation_idempotency_key"}))
public class SourceUploadSession {

  @Id private UUID id;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "source_document_id", nullable = false, updatable = false)
  private UUID sourceDocumentId;

  @Column(name = "source_version_id", nullable = false, unique = true, updatable = false)
  private UUID sourceVersionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UploadSessionStatus status;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "creation_idempotency_key", nullable = false, updatable = false, length = 128)
  private String creationIdempotencyKey;

  @Column(name = "creation_request_hash", nullable = false, updatable = false, length = 64)
  private String creationRequestHash;

  @Column(name = "completion_idempotency_key", length = 128)
  private String completionIdempotencyKey;

  @Column(name = "completion_request_hash", length = 64)
  private String completionRequestHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "staging_cleaned_at")
  private Instant stagingCleanedAt;

  protected SourceUploadSession() {}

  public SourceUploadSession(
      UUID id,
      UUID workspaceId,
      UUID sourceDocumentId,
      UUID sourceVersionId,
      Instant expiresAt,
      UUID createdBy,
      String creationIdempotencyKey,
      String creationRequestHash,
      Instant now) {
    this.id = id;
    this.workspaceId = workspaceId;
    this.sourceDocumentId = sourceDocumentId;
    this.sourceVersionId = sourceVersionId;
    this.status = UploadSessionStatus.PENDING;
    this.expiresAt = expiresAt;
    this.createdBy = createdBy;
    this.creationIdempotencyKey = creationIdempotencyKey;
    this.creationRequestHash = creationRequestHash;
    this.createdAt = now;
  }

  public void complete(String idempotencyKey, String requestHash, Instant now) {
    this.completionIdempotencyKey = idempotencyKey;
    this.completionRequestHash = requestHash;
    this.status = UploadSessionStatus.COMPLETED;
    this.completedAt = now;
  }

  public void expire() {
    if (status == UploadSessionStatus.PENDING || status == UploadSessionStatus.UPLOADING) {
      status = UploadSessionStatus.EXPIRED;
    }
  }

  public void markStagingCleaned(Instant now) {
    this.stagingCleanedAt = now;
  }

  public UUID id() {
    return id;
  }

  public UUID workspaceId() {
    return workspaceId;
  }

  public UUID sourceDocumentId() {
    return sourceDocumentId;
  }

  public UUID sourceVersionId() {
    return sourceVersionId;
  }

  public UploadSessionStatus status() {
    return status;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public UUID createdBy() {
    return createdBy;
  }

  public String creationRequestHash() {
    return creationRequestHash;
  }

  public String completionIdempotencyKey() {
    return completionIdempotencyKey;
  }

  public String completionRequestHash() {
    return completionRequestHash;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
