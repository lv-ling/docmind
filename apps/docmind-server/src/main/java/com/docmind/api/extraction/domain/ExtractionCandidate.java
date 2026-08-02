package com.docmind.api.extraction.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "extraction_candidate",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_extraction_candidate_position",
          columnNames = {"field_result_id", "candidate_position"}),
      @UniqueConstraint(
          name = "uq_extraction_candidate_field",
          columnNames = {"id", "field_result_id"})
    })
public class ExtractionCandidate {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "field_result_id", nullable = false, updatable = false)
  private UUID fieldResultId;

  @Column(name = "candidate_position", nullable = false, updatable = false)
  private int position;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "value_envelope", nullable = false, updatable = false)
  private JsonNode valueEnvelope;

  @Column(name = "masked_preview", nullable = false, updatable = false, length = 512)
  private String maskedPreview;

  @Column(nullable = false, updatable = false, precision = 5, scale = 4)
  private BigDecimal confidence;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected ExtractionCandidate() {}

  public ExtractionCandidate(
      UUID fieldResultId,
      int position,
      JsonNode valueEnvelope,
      String maskedPreview,
      BigDecimal confidence,
      Instant now) {
    this.fieldResultId = fieldResultId;
    this.position = position;
    this.valueEnvelope = valueEnvelope.deepCopy();
    this.maskedPreview = maskedPreview;
    this.confidence = confidence;
    this.createdAt = now;
  }

  public UUID id() { return id; }
  public UUID fieldResultId() { return fieldResultId; }
  public int position() { return position; }
  public JsonNode valueEnvelope() { return valueEnvelope.deepCopy(); }
  public String maskedPreview() { return maskedPreview; }
  public BigDecimal confidence() { return confidence; }
  public Instant createdAt() { return createdAt; }
}
