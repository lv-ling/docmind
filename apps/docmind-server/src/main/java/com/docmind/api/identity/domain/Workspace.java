package com.docmind.api.identity.domain;

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
    name = "workspace",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_workspace_creation_key",
          columnNames = {"created_by", "creation_idempotency_key"})
    })
public class Workspace {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, unique = true, length = 63)
  private String slug;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

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

  protected Workspace() {}

  public Workspace(
      String name,
      String slug,
      UUID createdBy,
      String creationIdempotencyKey,
      String creationRequestHash) {
    this.name = name;
    this.slug = slug;
    this.createdBy = createdBy;
    this.creationIdempotencyKey = creationIdempotencyKey;
    this.creationRequestHash = creationRequestHash;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  public String slug() {
    return slug;
  }

  public UUID createdBy() {
    return createdBy;
  }

  public String creationRequestHash() {
    return creationRequestHash;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
