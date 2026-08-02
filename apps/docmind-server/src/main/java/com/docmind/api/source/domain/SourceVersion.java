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
    name = "source_version",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_source_version_number",
            columnNames = {"source_document_id", "version_number"}))
public class SourceVersion {

  @Id private UUID id;

  @Column(name = "source_document_id", nullable = false, updatable = false)
  private UUID sourceDocumentId;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "version_number", nullable = false, updatable = false)
  private int versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SourceVersionStatus status;

  @Column(name = "original_file_name", nullable = false, updatable = false, length = 255)
  private String originalFileName;

  @Enumerated(EnumType.STRING)
  @Column(name = "file_type", nullable = false, updatable = false, length = 10)
  private SourceFileType fileType;

  @Column(name = "declared_mime_type", nullable = false, updatable = false, length = 255)
  private String declaredMimeType;

  @Column(name = "expected_size_bytes", nullable = false, updatable = false)
  private long expectedSizeBytes;

  @Column(name = "mime_type", length = 255)
  private String mimeType;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(length = 64)
  private String sha256;

  @Column(name = "upload_bucket", nullable = false, updatable = false, length = 63)
  private String uploadBucket;

  @Column(name = "upload_key", nullable = false, updatable = false, length = 1024)
  private String uploadKey;

  @Column(name = "object_bucket", nullable = false, updatable = false, length = 63)
  private String objectBucket;

  @Column(name = "object_key", nullable = false, updatable = false, length = 1024)
  private String objectKey;

  @Column(name = "object_etag", length = 255)
  private String objectEtag;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SourceVersion() {}

  public SourceVersion(
      UUID id,
      UUID sourceDocumentId,
      UUID workspaceId,
      int versionNumber,
      String originalFileName,
      SourceFileType fileType,
      String declaredMimeType,
      long expectedSizeBytes,
      String uploadBucket,
      String uploadKey,
      String objectBucket,
      String objectKey,
      UUID createdBy,
      Instant now) {
    this.id = id;
    this.sourceDocumentId = sourceDocumentId;
    this.workspaceId = workspaceId;
    this.versionNumber = versionNumber;
    this.status = SourceVersionStatus.UPLOADING;
    this.originalFileName = originalFileName;
    this.fileType = fileType;
    this.declaredMimeType = declaredMimeType;
    this.expectedSizeBytes = expectedSizeBytes;
    this.uploadBucket = uploadBucket;
    this.uploadKey = uploadKey;
    this.objectBucket = objectBucket;
    this.objectKey = objectKey;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void complete(String mimeType, long sizeBytes, String sha256, String etag, Instant now) {
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.sha256 = sha256;
    this.objectEtag = etag;
    this.status = SourceVersionStatus.UPLOADED;
    this.updatedAt = now;
  }

  public void markReady(Instant now) {
    if (status != SourceVersionStatus.UPLOADED && status != SourceVersionStatus.READY) {
      throw new IllegalStateException("Only uploaded source versions can become ready");
    }
    status = SourceVersionStatus.READY;
    failureCode = null;
    updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public UUID sourceDocumentId() {
    return sourceDocumentId;
  }

  public UUID workspaceId() {
    return workspaceId;
  }

  public int versionNumber() {
    return versionNumber;
  }

  public SourceVersionStatus status() {
    return status;
  }

  public String originalFileName() {
    return originalFileName;
  }

  public SourceFileType fileType() {
    return fileType;
  }

  public String declaredMimeType() {
    return declaredMimeType;
  }

  public long expectedSizeBytes() {
    return expectedSizeBytes;
  }

  public String mimeType() {
    return mimeType;
  }

  public Long sizeBytes() {
    return sizeBytes;
  }

  public String sha256() {
    return sha256;
  }

  public String objectBucket() {
    return objectBucket;
  }

  public String uploadBucket() {
    return uploadBucket;
  }

  public String uploadKey() {
    return uploadKey;
  }

  public String objectKey() {
    return objectKey;
  }

  public String objectEtag() {
    return objectEtag;
  }

  public String failureCode() {
    return failureCode;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
