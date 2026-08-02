package com.docmind.api.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_preview")
public class SourcePreview {

  @Id private UUID id;

  @Column(name = "source_version_id", nullable = false, unique = true, updatable = false)
  private UUID sourceVersionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SourcePreviewStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SourcePreviewFormat format;

  @Column(name = "object_bucket", length = 63)
  private String objectBucket;

  @Column(name = "object_key", length = 1024)
  private String objectKey;

  @Column(name = "page_count")
  private Integer pageCount;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected SourcePreview() {}

  public SourcePreview(UUID id, UUID sourceVersionId, Instant now) {
    this.id = id;
    this.sourceVersionId = sourceVersionId;
    this.status = SourcePreviewStatus.QUEUED;
    this.format = SourcePreviewFormat.PDF;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public UUID sourceVersionId() {
    return sourceVersionId;
  }

  public SourcePreviewStatus status() {
    return status;
  }

  public SourcePreviewFormat format() {
    return format;
  }

  public String objectBucket() {
    return objectBucket;
  }

  public String objectKey() {
    return objectKey;
  }

  public Integer pageCount() {
    return pageCount;
  }

  public String failureCode() {
    return failureCode;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant completedAt() {
    return completedAt;
  }

  public void start(Instant now) {
    if (status == SourcePreviewStatus.READY) return;
    status = SourcePreviewStatus.PROCESSING;
    failureCode = null;
    updatedAt = now;
  }

  public void complete(
      String bucket, String key, Integer resolvedPageCount, Instant now) {
    objectBucket = bucket;
    objectKey = key;
    pageCount = resolvedPageCount;
    failureCode = null;
    status = SourcePreviewStatus.READY;
    updatedAt = now;
    completedAt = now;
  }

  public void fail(String code, Instant now) {
    if (status == SourcePreviewStatus.READY) return;
    status = SourcePreviewStatus.FAILED;
    failureCode = code;
    updatedAt = now;
    completedAt = now;
  }
}
