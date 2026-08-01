package com.docmind.api.schema.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "schema_template",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_schema_template_creation_key",
            columnNames = {"workspace_id", "created_by", "creation_idempotency_key"}))
public class SchemaTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(nullable = false, updatable = false, length = 100)
  private String name;

  @Column(nullable = false, updatable = false, length = 2000)
  private String description;

  @Column(name = "current_schema_version_id", nullable = false, updatable = false)
  private UUID currentSchemaVersionId;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "updated_by", nullable = false)
  private UUID updatedBy;

  @Column(name = "creation_idempotency_key", nullable = false, updatable = false, length = 128)
  private String creationIdempotencyKey;

  @Column(name = "creation_request_hash", nullable = false, updatable = false, length = 64)
  private String creationRequestHash;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected SchemaTemplate() {}

  public SchemaTemplate(
      UUID workspaceId,
      String name,
      String description,
      UUID schemaVersionId,
      UUID createdBy,
      String idempotencyKey,
      String requestHash,
      Instant now) {
    this.workspaceId = workspaceId;
    this.name = name;
    this.description = description;
    this.currentSchemaVersionId = schemaVersionId;
    this.createdBy = createdBy;
    this.updatedBy = createdBy;
    this.creationIdempotencyKey = idempotencyKey;
    this.creationRequestHash = requestHash;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID id() { return id; }
  public UUID workspaceId() { return workspaceId; }
  public String name() { return name; }
  public String description() { return description; }
  public UUID currentSchemaVersionId() { return currentSchemaVersionId; }
  public UUID createdBy() { return createdBy; }
  public UUID updatedBy() { return updatedBy; }
  public String creationRequestHash() { return creationRequestHash; }
  public Instant createdAt() { return createdAt; }
  public Instant updatedAt() { return updatedAt; }
}
