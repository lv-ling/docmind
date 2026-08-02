package com.docmind.api.extraction.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "sensitive_token",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_sensitive_token_run_token",
            columnNames = {"extraction_run_id", "token"}))
public class SensitiveToken {

  @Id private UUID id;

  @Column(name = "extraction_run_id", nullable = false, updatable = false)
  private UUID extractionRunId;

  @Column(name = "source_version_id", nullable = false, updatable = false)
  private UUID sourceVersionId;

  @Column(nullable = false, updatable = false, length = 160)
  private String token;

  @Column(name = "data_type", nullable = false, updatable = false, length = 40)
  private String dataType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "value_envelope", nullable = false, updatable = false)
  private JsonNode valueEnvelope;

  @Column(name = "masked_preview", nullable = false, updatable = false, length = 100)
  private String maskedPreview;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected SensitiveToken() {}

  public SensitiveToken(
      UUID id,
      UUID extractionRunId,
      UUID sourceVersionId,
      String token,
      String dataType,
      JsonNode valueEnvelope,
      String maskedPreview,
      Instant createdAt) {
    this.id = id;
    this.extractionRunId = extractionRunId;
    this.sourceVersionId = sourceVersionId;
    this.token = token;
    this.dataType = dataType;
    this.valueEnvelope = valueEnvelope.deepCopy();
    this.maskedPreview = maskedPreview;
    this.createdAt = createdAt;
  }

  public UUID id() { return id; }
  public UUID extractionRunId() { return extractionRunId; }
  public UUID sourceVersionId() { return sourceVersionId; }
  public String token() { return token; }
  public String dataType() { return dataType; }
  public JsonNode valueEnvelope() { return valueEnvelope.deepCopy(); }
  public String maskedPreview() { return maskedPreview; }
  public Instant createdAt() { return createdAt; }
}
