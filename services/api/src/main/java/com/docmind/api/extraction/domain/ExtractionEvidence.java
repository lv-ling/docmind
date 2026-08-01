package com.docmind.api.extraction.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "extraction_evidence")
public class ExtractionEvidence {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "field_result_id", nullable = false, updatable = false)
  private UUID fieldResultId;

  @Column(name = "candidate_id", updatable = false)
  private UUID candidateId;

  @Column(name = "source_version_id", nullable = false, updatable = false)
  private UUID sourceVersionId;

  @Column(name = "evidence_position", nullable = false, updatable = false)
  private int position;

  @Column(name = "page_number", updatable = false)
  private Integer pageNumber;

  @Column(name = "node_id", nullable = false, updatable = false, length = 255)
  private String nodeId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "text_envelope", nullable = false, updatable = false)
  private JsonNode textEnvelope;

  @Column(name = "masked_preview", nullable = false, updatable = false, length = 1000)
  private String maskedPreview;

  @Column(name = "start_offset", updatable = false)
  private Integer startOffset;

  @Column(name = "end_offset", updatable = false)
  private Integer endOffset;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected ExtractionEvidence() {}

  public ExtractionEvidence(
      UUID fieldResultId,
      UUID candidateId,
      UUID sourceVersionId,
      int position,
      Integer pageNumber,
      String nodeId,
      JsonNode textEnvelope,
      String maskedPreview,
      Integer startOffset,
      Integer endOffset,
      Instant now) {
    this.fieldResultId = fieldResultId;
    this.candidateId = candidateId;
    this.sourceVersionId = sourceVersionId;
    this.position = position;
    this.pageNumber = pageNumber;
    this.nodeId = nodeId;
    this.textEnvelope = textEnvelope.deepCopy();
    this.maskedPreview = maskedPreview;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
    this.createdAt = now;
  }

  public UUID id() { return id; }
  public UUID fieldResultId() { return fieldResultId; }
  public UUID candidateId() { return candidateId; }
  public UUID sourceVersionId() { return sourceVersionId; }
  public int position() { return position; }
  public Integer pageNumber() { return pageNumber; }
  public String nodeId() { return nodeId; }
  public JsonNode textEnvelope() { return textEnvelope.deepCopy(); }
  public String maskedPreview() { return maskedPreview; }
  public Integer startOffset() { return startOffset; }
  public Integer endOffset() { return endOffset; }
}
