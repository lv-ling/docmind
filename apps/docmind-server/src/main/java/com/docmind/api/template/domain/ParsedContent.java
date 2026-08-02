package com.docmind.api.template.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "parsed_content")
public class ParsedContent {
  @Id private UUID id;

  @Column(name = "source_version_id", nullable = false, updatable = false)
  private UUID sourceVersionId;

  @Column(name = "parser_name", nullable = false, updatable = false, length = 100)
  private String parserName;

  @Column(name = "parser_version", nullable = false, updatable = false, length = 100)
  private String parserVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "document_model_envelope", nullable = false, updatable = false)
  private JsonNode documentModelEnvelope;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected ParsedContent() {}

  public ParsedContent(
      UUID id,
      UUID sourceVersionId,
      String parserName,
      String parserVersion,
      JsonNode documentModelEnvelope,
      Instant now) {
    this.id = id;
    this.sourceVersionId = sourceVersionId;
    this.parserName = parserName;
    this.parserVersion = parserVersion;
    this.documentModelEnvelope = documentModelEnvelope.deepCopy();
    this.createdAt = now;
  }

  public UUID id() { return id; }
  public UUID sourceVersionId() { return sourceVersionId; }
  public String parserName() { return parserName; }
  public String parserVersion() { return parserVersion; }
  public JsonNode documentModelEnvelope() { return documentModelEnvelope.deepCopy(); }
}
