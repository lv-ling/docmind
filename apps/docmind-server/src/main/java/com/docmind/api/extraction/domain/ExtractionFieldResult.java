package com.docmind.api.extraction.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "extraction_field_result",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_extraction_field_schema_field",
          columnNames = {"extraction_run_id", "schema_field_id"}),
      @UniqueConstraint(
          name = "uq_extraction_field_json_path",
          columnNames = {"extraction_run_id", "json_path"})
    })
public class ExtractionFieldResult {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "extraction_run_id", nullable = false, updatable = false)
  private UUID extractionRunId;

  @Column(name = "schema_field_id", nullable = false, updatable = false)
  private UUID schemaFieldId;

  @Column(name = "json_path", nullable = false, updatable = false, length = 500)
  private String jsonPath;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "value_envelope", nullable = false)
  private JsonNode valueEnvelope;

  @Column(name = "masked_preview", nullable = false, length = 512)
  private String maskedPreview;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_source", nullable = false, length = 20)
  private ExtractionValueSource valueSource;

  @Enumerated(EnumType.STRING)
  @Column(name = "missing_reason", length = 40)
  private ExtractionMissingReason missingReason;

  @Column(precision = 5, scale = 4)
  private BigDecimal confidence;

  @Column(name = "needs_review", nullable = false)
  private boolean needsReview;

  @Enumerated(EnumType.STRING)
  @Column(name = "review_status", nullable = false, length = 20)
  private FieldReviewStatus reviewStatus;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reviewed_value_envelope")
  private JsonNode reviewedValueEnvelope;

  @Column(name = "review_reason", length = 2000)
  private String reviewReason;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long revision;

  protected ExtractionFieldResult() {}

  public ExtractionFieldResult(
      UUID extractionRunId,
      UUID schemaFieldId,
      String jsonPath,
      JsonNode valueEnvelope,
      String maskedPreview,
      ExtractionValueSource valueSource,
      ExtractionMissingReason missingReason,
      BigDecimal confidence,
      boolean needsReview,
      Instant now) {
    this.extractionRunId = extractionRunId;
    this.schemaFieldId = schemaFieldId;
    this.jsonPath = jsonPath;
    this.valueEnvelope = valueEnvelope.deepCopy();
    this.maskedPreview = maskedPreview;
    this.valueSource = valueSource;
    this.missingReason = missingReason;
    this.confidence = confidence;
    this.needsReview = needsReview;
    this.reviewStatus = FieldReviewStatus.PENDING;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID id() { return id; }
  public UUID extractionRunId() { return extractionRunId; }
  public UUID schemaFieldId() { return schemaFieldId; }
  public String jsonPath() { return jsonPath; }
  public JsonNode valueEnvelope() { return valueEnvelope.deepCopy(); }
  public String maskedPreview() { return maskedPreview; }
  public ExtractionValueSource valueSource() { return valueSource; }
  public ExtractionMissingReason missingReason() { return missingReason; }
  public BigDecimal confidence() { return confidence; }
  public boolean needsReview() { return needsReview; }
  public FieldReviewStatus reviewStatus() { return reviewStatus; }
  public JsonNode reviewedValueEnvelope() {
    return reviewedValueEnvelope == null ? null : reviewedValueEnvelope.deepCopy();
  }
  public String reviewReason() { return reviewReason; }
  public UUID reviewedBy() { return reviewedBy; }
  public Instant reviewedAt() { return reviewedAt; }

  public void review(
      FieldReviewStatus nextStatus,
      JsonNode encryptedReviewedValue,
      String reason,
      UUID reviewerId,
      Instant now) {
    if (nextStatus == null || nextStatus == FieldReviewStatus.PENDING) {
      throw new IllegalArgumentException("Review status must be terminal");
    }
    if (nextStatus == FieldReviewStatus.MODIFIED && encryptedReviewedValue == null) {
      throw new IllegalArgumentException("Modified review requires a value");
    }
    this.reviewStatus = nextStatus;
    this.reviewedValueEnvelope =
        nextStatus == FieldReviewStatus.MODIFIED ? encryptedReviewedValue.deepCopy() : null;
    this.reviewReason = reason;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
    this.updatedAt = now;
  }
}
