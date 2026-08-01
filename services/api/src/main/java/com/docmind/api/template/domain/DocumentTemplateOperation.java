package com.docmind.api.template.domain;

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
    name = "document_template_operation",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_document_template_operation_key",
            columnNames = {"actor_id", "operation_type", "idempotency_key"}))
public class DocumentTemplateOperation {
  @Id private UUID id;
  @Column(name = "template_id", nullable = false, updatable = false)
  private UUID templateId;
  @Column(name = "result_version_id", nullable = false, updatable = false)
  private UUID resultVersionId;
  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, updatable = false, length = 30)
  private DocumentTemplateOperationType operationType;
  @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
  private String idempotencyKey;
  @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
  private String requestHash;
  @Column(name = "actor_id", nullable = false, updatable = false)
  private UUID actorId;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected DocumentTemplateOperation() {}

  public DocumentTemplateOperation(
      UUID templateId,
      UUID resultVersionId,
      DocumentTemplateOperationType type,
      String idempotencyKey,
      String requestHash,
      UUID actorId,
      Instant now) {
    this.id = UUID.randomUUID();
    this.templateId = templateId;
    this.resultVersionId = resultVersionId;
    this.operationType = type;
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.actorId = actorId;
    this.createdAt = now;
  }

  public UUID templateId() { return templateId; }
  public UUID resultVersionId() { return resultVersionId; }
  public String requestHash() { return requestHash; }
}
