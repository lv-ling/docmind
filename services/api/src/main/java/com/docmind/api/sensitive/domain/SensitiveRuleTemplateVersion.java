package com.docmind.api.sensitive.domain;

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

@Entity
@Table(
    name = "sensitive_rule_template_version",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_sensitive_rule_template_version_number",
          columnNames = {"template_id", "version_number"}),
      @UniqueConstraint(
          name = "uq_sensitive_rule_template_version_creation_key",
          columnNames = {"template_id", "created_by", "creation_idempotency_key"})
    })
public class SensitiveRuleTemplateVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "template_id", nullable = false, updatable = false)
  private UUID templateId;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "version_number", nullable = false, updatable = false)
  private int versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SensitiveRuleTemplateVersionStatus status;

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

  protected SensitiveRuleTemplateVersion() {}

  public SensitiveRuleTemplateVersion(
      UUID templateId,
      UUID workspaceId,
      int versionNumber,
      String changeSummary,
      UUID createdBy,
      String idempotencyKey,
      String requestHash,
      Instant now) {
    this.templateId = templateId;
    this.workspaceId = workspaceId;
    this.versionNumber = versionNumber;
    this.status = SensitiveRuleTemplateVersionStatus.PUBLISHED;
    this.changeSummary = changeSummary;
    this.createdBy = createdBy;
    this.creationIdempotencyKey = idempotencyKey;
    this.creationRequestHash = requestHash;
    this.createdAt = now;
    this.publishedAt = now;
  }

  public void supersede() { this.status = SensitiveRuleTemplateVersionStatus.SUPERSEDED; }

  public UUID id() { return id; }
  public UUID templateId() { return templateId; }
  public UUID workspaceId() { return workspaceId; }
  public int versionNumber() { return versionNumber; }
  public SensitiveRuleTemplateVersionStatus status() { return status; }
  public String changeSummary() { return changeSummary; }
  public UUID createdBy() { return createdBy; }
  public String creationRequestHash() { return creationRequestHash; }
  public Instant createdAt() { return createdAt; }
  public Instant publishedAt() { return publishedAt; }
}
