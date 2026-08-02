package com.docmind.api.schema.domain;

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
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "extraction_schema_version",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_extraction_schema_version_number",
          columnNames = {"schema_id", "version_number"}),
      @UniqueConstraint(
          name = "uq_extraction_schema_version_creation_key",
          columnNames = {"schema_id", "created_by", "creation_idempotency_key"})
    })
public class ExtractionSchemaVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "schema_id", nullable = false, updatable = false)
  private UUID schemaId;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "version_number", nullable = false, updatable = false)
  private int versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SchemaVersionStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "json_schema", nullable = false, updatable = false)
  private JsonNode jsonSchema;

  @Column(name = "change_summary", nullable = false, updatable = false, length = 1000)
  private String changeSummary;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "creation_idempotency_key", updatable = false, length = 128)
  private String creationIdempotencyKey;

  @Column(name = "creation_request_hash", nullable = false, updatable = false, length = 64)
  private String creationRequestHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "published_at", nullable = false, updatable = false)
  private Instant publishedAt;

  protected ExtractionSchemaVersion() {}

  public ExtractionSchemaVersion(
      UUID schemaId,
      UUID workspaceId,
      int versionNumber,
      JsonNode jsonSchema,
      String changeSummary,
      UUID createdBy,
      String idempotencyKey,
      String requestHash,
      Instant now) {
    this.schemaId = schemaId;
    this.workspaceId = workspaceId;
    this.versionNumber = versionNumber;
    this.status = SchemaVersionStatus.PUBLISHED;
    this.jsonSchema = jsonSchema.deepCopy();
    this.changeSummary = changeSummary;
    this.createdBy = createdBy;
    this.creationIdempotencyKey = idempotencyKey;
    this.creationRequestHash = requestHash;
    this.createdAt = now;
    this.publishedAt = now;
  }

  public void supersede() { this.status = SchemaVersionStatus.SUPERSEDED; }

  public UUID id() { return id; }
  public UUID schemaId() { return schemaId; }
  public UUID workspaceId() { return workspaceId; }
  public int versionNumber() { return versionNumber; }
  public SchemaVersionStatus status() { return status; }
  public JsonNode jsonSchema() { return jsonSchema.deepCopy(); }
  public String changeSummary() { return changeSummary; }
  public UUID createdBy() { return createdBy; }
  public String creationRequestHash() { return creationRequestHash; }
  public Instant createdAt() { return createdAt; }
  public Instant publishedAt() { return publishedAt; }
}
