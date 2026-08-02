package com.docmind.api.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_document")
public class SourceDocument {

  @Id private UUID id;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "current_version_id")
  private UUID currentVersionId;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "updated_by", nullable = false)
  private UUID updatedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected SourceDocument() {}

  public SourceDocument(UUID id, UUID workspaceId, String name, UUID createdBy, Instant now) {
    this.id = id;
    this.workspaceId = workspaceId;
    this.name = name;
    this.createdBy = createdBy;
    this.updatedBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void advanceTo(UUID versionId, UUID userId, Instant now) {
    this.currentVersionId = versionId;
    this.updatedBy = userId;
    this.updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public UUID workspaceId() {
    return workspaceId;
  }

  public String name() {
    return name;
  }

  public UUID currentVersionId() {
    return currentVersionId;
  }

  public UUID createdBy() {
    return createdBy;
  }

  public UUID updatedBy() {
    return updatedBy;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
