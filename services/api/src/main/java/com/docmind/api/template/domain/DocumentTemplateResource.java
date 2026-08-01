package com.docmind.api.template.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_template_resource")
public class DocumentTemplateResource {
  @Id private UUID id;

  @Column(name = "template_version_id", nullable = false, updatable = false)
  private UUID templateVersionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private TemplateResourceKind kind;

  @Column(name = "content_type", nullable = false, updatable = false, length = 255)
  private String contentType;

  @Column(name = "byte_size", nullable = false, updatable = false)
  private long byteSize;

  @Column(nullable = false, updatable = false, length = 64)
  private String sha256;

  @Column(name = "object_bucket", nullable = false, updatable = false, length = 63)
  private String objectBucket;

  @Column(name = "object_key", nullable = false, updatable = false, length = 1024)
  private String objectKey;

  @Column(name = "original_filename", nullable = false, updatable = false, length = 255)
  private String originalFilename;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected DocumentTemplateResource() {}

  public DocumentTemplateResource(
      UUID id,
      UUID versionId,
      TemplateResourceKind kind,
      String contentType,
      long byteSize,
      String sha256,
      String bucket,
      String key,
      String filename,
      Instant now) {
    this.id = id;
    this.templateVersionId = versionId;
    this.kind = kind;
    this.contentType = contentType;
    this.byteSize = byteSize;
    this.sha256 = sha256;
    this.objectBucket = bucket;
    this.objectKey = key;
    this.originalFilename = filename;
    this.createdAt = now;
  }

  public UUID id() { return id; }
  public UUID templateVersionId() { return templateVersionId; }
  public TemplateResourceKind kind() { return kind; }
  public String contentType() { return contentType; }
  public long byteSize() { return byteSize; }
  public String sha256() { return sha256; }
  public String objectBucket() { return objectBucket; }
  public String objectKey() { return objectKey; }
  public String originalFilename() { return originalFilename; }
}
