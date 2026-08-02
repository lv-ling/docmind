package com.docmind.api.template.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "document_template_version",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_document_template_version_number",
            columnNames = {"template_id", "version_number"}))
public class DocumentTemplateVersion {
  @Id private UUID id;

  @Column(name = "template_id", nullable = false, updatable = false)
  private UUID templateId;

  @Column(name = "workspace_id", nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(name = "source_version_id", nullable = false, updatable = false)
  private UUID sourceVersionId;

  @Column(name = "parsed_content_id", nullable = false, updatable = false)
  private UUID parsedContentId;

  @Column(name = "resource_version_id", nullable = false, updatable = false)
  private UUID resourceVersionId;

  @Column(name = "version_number", nullable = false, updatable = false)
  private int versionNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TemplateVersionStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "document_model_envelope", nullable = false)
  private JsonNode documentModelEnvelope;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "html_envelope", nullable = false)
  private JsonNode htmlEnvelope;

  @Column(name = "css_text", nullable = false, columnDefinition = "TEXT")
  private String cssText;

  @Column(name = "sanitization_policy_version", nullable = false, length = 50)
  private String sanitizationPolicyVersion;

  @Column(name = "change_summary", nullable = false, length = 1000)
  private String changeSummary;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "diff_envelope", nullable = false)
  private JsonNode diffEnvelope;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "published_by")
  private UUID publishedBy;

  @Version
  @Column(nullable = false)
  private long revision;

  protected DocumentTemplateVersion() {}

  public DocumentTemplateVersion(
      UUID id,
      UUID templateId,
      UUID workspaceId,
      UUID sourceVersionId,
      UUID parsedContentId,
      UUID resourceVersionId,
      int versionNumber,
      TemplateVersionStatus status,
      JsonNode documentModelEnvelope,
      JsonNode htmlEnvelope,
      String cssText,
      String policyVersion,
      String changeSummary,
      JsonNode diffEnvelope,
      UUID createdBy,
      Instant now) {
    this.id = id;
    this.templateId = templateId;
    this.workspaceId = workspaceId;
    this.sourceVersionId = sourceVersionId;
    this.parsedContentId = parsedContentId;
    this.resourceVersionId = resourceVersionId;
    this.versionNumber = versionNumber;
    this.status = status;
    this.documentModelEnvelope = documentModelEnvelope.deepCopy();
    this.htmlEnvelope = htmlEnvelope.deepCopy();
    this.cssText = cssText;
    this.sanitizationPolicyVersion = policyVersion;
    this.changeSummary = changeSummary;
    this.diffEnvelope = diffEnvelope.deepCopy();
    this.createdBy = createdBy;
    this.createdAt = now;
  }

  public UUID id() { return id; }
  public UUID templateId() { return templateId; }
  public UUID workspaceId() { return workspaceId; }
  public UUID sourceVersionId() { return sourceVersionId; }
  public UUID parsedContentId() { return parsedContentId; }
  public UUID resourceVersionId() { return resourceVersionId; }
  public int versionNumber() { return versionNumber; }
  public TemplateVersionStatus status() { return status; }
  public JsonNode documentModelEnvelope() { return documentModelEnvelope.deepCopy(); }
  public JsonNode htmlEnvelope() { return htmlEnvelope.deepCopy(); }
  public String cssText() { return cssText; }
  public String sanitizationPolicyVersion() { return sanitizationPolicyVersion; }
  public String changeSummary() { return changeSummary; }
  public JsonNode diffEnvelope() { return diffEnvelope.deepCopy(); }
  public Instant createdAt() { return createdAt; }
  public UUID createdBy() { return createdBy; }
  public Instant publishedAt() { return publishedAt; }

  public void beginChecking() {
    if (status == TemplateVersionStatus.GENERATED) status = TemplateVersionStatus.CHECKING;
  }

  public void publish(UUID userId, Instant now) {
    if (status != TemplateVersionStatus.GENERATED && status != TemplateVersionStatus.CHECKING) {
      throw new IllegalStateException("Template version cannot be published");
    }
    status = TemplateVersionStatus.PUBLISHED;
    publishedAt = now;
    publishedBy = userId;
  }

  public void supersede() {
    if (status == TemplateVersionStatus.PUBLISHED) status = TemplateVersionStatus.SUPERSEDED;
  }
}
