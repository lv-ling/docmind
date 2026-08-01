package com.docmind.api.extraction.domain;

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
    name = "extraction_review_operation",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_extraction_review_operation_key",
            columnNames = {"actor_id", "operation_type", "idempotency_key"}))
public class ExtractionReviewOperation {

  @Id private UUID id;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "extraction_run_id", nullable = false, updatable = false)
  private UUID extractionRunId;

  @Column(name = "field_result_id", updatable = false)
  private UUID fieldResultId;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, updatable = false, length = 30)
  private ExtractionReviewOperationType operationType;

  @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
  private String requestHash;

  @Column(name = "actor_id", nullable = false, updatable = false)
  private UUID actorId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected ExtractionReviewOperation() {}

  public ExtractionReviewOperation(
      UUID workspaceId,
      UUID extractionRunId,
      UUID fieldResultId,
      ExtractionReviewOperationType operationType,
      String idempotencyKey,
      String requestHash,
      UUID actorId,
      Instant now) {
    this.id = UUID.randomUUID();
    this.workspaceId = workspaceId;
    this.extractionRunId = extractionRunId;
    this.fieldResultId = fieldResultId;
    this.operationType = operationType;
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.actorId = actorId;
    this.createdAt = now;
  }

  public UUID extractionRunId() { return extractionRunId; }
  public UUID fieldResultId() { return fieldResultId; }
  public String requestHash() { return requestHash; }
}
