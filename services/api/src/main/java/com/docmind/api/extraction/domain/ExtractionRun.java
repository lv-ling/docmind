package com.docmind.api.extraction.domain;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "extraction_run",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_extraction_run_job", columnNames = "job_id"),
      @UniqueConstraint(
          name = "uq_extraction_run_creation_key",
          columnNames = {"source_version_id", "created_by", "creation_idempotency_key"})
    })
public class ExtractionRun {

  @Id private UUID id;

  @Column(name = "job_id", nullable = false, updatable = false)
  private UUID jobId;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "source_version_id", nullable = false, updatable = false)
  private UUID sourceVersionId;

  @Column(name = "schema_version_id", nullable = false, updatable = false)
  private UUID schemaVersionId;

  @Column(name = "sensitive_rule_template_version_id", updatable = false)
  private UUID sensitiveRuleTemplateVersionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ExtractionRunStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "result_data_envelope")
  private JsonNode resultDataEnvelope;

  @Column(name = "contains_sensitive_values", nullable = false)
  private boolean containsSensitiveValues;

  @Column(name = "model_provider", length = 100)
  private String modelProvider;

  @Column(name = "model_name", length = 200)
  private String modelName;

  @Column(name = "prompt_version", length = 100)
  private String promptVersion;

  @Column(name = "input_tokens")
  private Integer inputTokens;

  @Column(name = "output_tokens")
  private Integer outputTokens;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "validation_errors", nullable = false)
  private JsonNode validationErrors;

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

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "approved_by")
  private UUID approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approval_note", length = 2000)
  private String approvalNote;

  @Version
  @Column(nullable = false)
  private long revision;

  protected ExtractionRun() {}

  public ExtractionRun(
      UUID id,
      UUID jobId,
      UUID workspaceId,
      UUID sourceVersionId,
      UUID schemaVersionId,
      UUID sensitiveRuleTemplateVersionId,
      UUID createdBy,
      String idempotencyKey,
      String requestHash,
      JsonNode emptyValidationErrors,
      Instant now) {
    this.id = id;
    this.jobId = jobId;
    this.workspaceId = workspaceId;
    this.sourceVersionId = sourceVersionId;
    this.schemaVersionId = schemaVersionId;
    this.sensitiveRuleTemplateVersionId = sensitiveRuleTemplateVersionId;
    this.status = ExtractionRunStatus.QUEUED;
    this.createdBy = createdBy;
    this.creationIdempotencyKey = idempotencyKey;
    this.creationRequestHash = requestHash;
    this.validationErrors = emptyValidationErrors.deepCopy();
    this.createdAt = now;
  }

  public UUID id() { return id; }
  public UUID jobId() { return jobId; }
  public UUID workspaceId() { return workspaceId; }
  public UUID sourceVersionId() { return sourceVersionId; }
  public UUID schemaVersionId() { return schemaVersionId; }
  public UUID sensitiveRuleTemplateVersionId() { return sensitiveRuleTemplateVersionId; }
  public ExtractionRunStatus status() { return status; }
  public boolean containsSensitiveValues() { return containsSensitiveValues; }
  public String modelProvider() { return modelProvider; }
  public String modelName() { return modelName; }
  public String promptVersion() { return promptVersion; }
  public Integer inputTokens() { return inputTokens; }
  public Integer outputTokens() { return outputTokens; }
  public JsonNode validationErrors() { return validationErrors.deepCopy(); }
  public String failureCode() { return failureCode; }
  public UUID createdBy() { return createdBy; }
  public String creationRequestHash() { return creationRequestHash; }
  public Instant createdAt() { return createdAt; }
  public Instant completedAt() { return completedAt; }
  public UUID approvedBy() { return approvedBy; }
  public Instant approvedAt() { return approvedAt; }
  public String approvalNote() { return approvalNote; }

  public JsonNode resultDataEnvelope() {
    return resultDataEnvelope == null ? null : resultDataEnvelope.deepCopy();
  }

  public boolean start() {
    if (status == ExtractionRunStatus.REVIEW_REQUIRED
        || status == ExtractionRunStatus.APPROVED
        || status == ExtractionRunStatus.FAILED) {
      return false;
    }
    status = ExtractionRunStatus.RUNNING;
    failureCode = null;
    completedAt = null;
    return true;
  }

  public void markRetrying(String safeFailureCode) {
    if (status == ExtractionRunStatus.REVIEW_REQUIRED || status == ExtractionRunStatus.APPROVED) {
      return;
    }
    status = ExtractionRunStatus.RETRYING;
    failureCode = safeFailureCode;
    completedAt = null;
  }

  public void markFailed(String safeFailureCode, Instant now) {
    if (status == ExtractionRunStatus.REVIEW_REQUIRED || status == ExtractionRunStatus.APPROVED) {
      return;
    }
    status = ExtractionRunStatus.FAILED;
    failureCode = safeFailureCode;
    completedAt = now;
  }

  public void completeForReview(
      JsonNode encryptedResult,
      boolean hasSensitiveValues,
      String provider,
      String model,
      String usedPromptVersion,
      Integer usedInputTokens,
      Integer usedOutputTokens,
      JsonNode safeValidationErrors,
      Instant now) {
    if (status != ExtractionRunStatus.RUNNING) {
      throw new IllegalStateException("Extraction run is not running");
    }
    resultDataEnvelope = encryptedResult.deepCopy();
    containsSensitiveValues = hasSensitiveValues;
    modelProvider = provider;
    modelName = model;
    promptVersion = usedPromptVersion;
    inputTokens = usedInputTokens;
    outputTokens = usedOutputTokens;
    validationErrors = safeValidationErrors.deepCopy();
    failureCode = null;
    status = ExtractionRunStatus.REVIEW_REQUIRED;
    completedAt = now;
  }

  public void approve(JsonNode encryptedFinalResult, UUID reviewerId, String note, Instant now) {
    if (status != ExtractionRunStatus.REVIEW_REQUIRED) {
      throw new IllegalStateException("Extraction run is not ready for approval");
    }
    resultDataEnvelope = encryptedFinalResult.deepCopy();
    status = ExtractionRunStatus.APPROVED;
    approvedBy = reviewerId;
    approvedAt = now;
    approvalNote = note;
    completedAt = now;
  }
}
