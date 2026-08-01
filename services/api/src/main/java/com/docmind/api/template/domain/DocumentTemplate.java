package com.docmind.api.template.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "document_template",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_document_template_job", columnNames = "conversion_job_id"),
      @UniqueConstraint(
          name = "uq_document_template_creation_key",
          columnNames = {"source_version_id", "created_by", "creation_idempotency_key"})
    })
public class DocumentTemplate {

  @Id private UUID id;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "source_document_id", nullable = false, updatable = false)
  private UUID sourceDocumentId;

  @Column(name = "source_version_id", nullable = false, updatable = false)
  private UUID sourceVersionId;

  @Column(name = "conversion_job_id", nullable = false, updatable = false)
  private UUID conversionJobId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "current_version_id")
  private UUID currentVersionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "conversion_status", nullable = false, length = 20)
  private TemplateConversionStatus conversionStatus;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "creation_idempotency_key", nullable = false, updatable = false, length = 128)
  private String creationIdempotencyKey;

  @Column(name = "creation_request_hash", nullable = false, updatable = false, length = 64)
  private String creationRequestHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by", nullable = false)
  private UUID updatedBy;

  @Version
  @Column(nullable = false)
  private long revision;

  protected DocumentTemplate() {}

  public DocumentTemplate(
      UUID id,
      UUID workspaceId,
      UUID sourceDocumentId,
      UUID sourceVersionId,
      UUID conversionJobId,
      String name,
      UUID createdBy,
      String idempotencyKey,
      String requestHash,
      Instant now) {
    this.id = id;
    this.workspaceId = workspaceId;
    this.sourceDocumentId = sourceDocumentId;
    this.sourceVersionId = sourceVersionId;
    this.conversionJobId = conversionJobId;
    this.name = name;
    this.conversionStatus = TemplateConversionStatus.QUEUED;
    this.createdBy = createdBy;
    this.creationIdempotencyKey = idempotencyKey;
    this.creationRequestHash = requestHash;
    this.createdAt = now;
    this.updatedAt = now;
    this.updatedBy = createdBy;
  }

  public UUID id() { return id; }
  public UUID workspaceId() { return workspaceId; }
  public UUID sourceDocumentId() { return sourceDocumentId; }
  public UUID sourceVersionId() { return sourceVersionId; }
  public UUID conversionJobId() { return conversionJobId; }
  public String name() { return name; }
  public UUID currentVersionId() { return currentVersionId; }
  public TemplateConversionStatus conversionStatus() { return conversionStatus; }
  public String failureCode() { return failureCode; }
  public UUID createdBy() { return createdBy; }
  public String creationRequestHash() { return creationRequestHash; }
  public Instant createdAt() { return createdAt; }
  public Instant updatedAt() { return updatedAt; }
  public UUID updatedBy() { return updatedBy; }

  public boolean start() {
    if (conversionStatus == TemplateConversionStatus.READY
        || conversionStatus == TemplateConversionStatus.FAILED) return false;
    conversionStatus = TemplateConversionStatus.RUNNING;
    failureCode = null;
    return true;
  }

  public void retry(String code, UUID actorId, Instant now) {
    if (conversionStatus == TemplateConversionStatus.READY) return;
    conversionStatus = TemplateConversionStatus.RETRYING;
    failureCode = code;
    updatedAt = now;
    updatedBy = actorId;
  }

  public void fail(String code, UUID actorId, Instant now) {
    if (conversionStatus == TemplateConversionStatus.READY) return;
    conversionStatus = TemplateConversionStatus.FAILED;
    failureCode = code;
    updatedAt = now;
    updatedBy = actorId;
  }

  public void complete(UUID versionId, UUID actorId, Instant now) {
    currentVersionId = versionId;
    conversionStatus = TemplateConversionStatus.READY;
    failureCode = null;
    updatedAt = now;
    updatedBy = actorId;
  }

  public void moveToVersion(UUID versionId, UUID actorId, Instant now) {
    currentVersionId = versionId;
    updatedAt = now;
    updatedBy = actorId;
  }
}
