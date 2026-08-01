package com.docmind.api.template.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
    name = "document_conversion_warning",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_document_conversion_warning_position",
            columnNames = {"template_version_id", "warning_position"}))
public class DocumentConversionWarning {
  @Id private UUID id;

  @Column(name = "template_version_id", nullable = false, updatable = false)
  private UUID templateVersionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private ConversionWarningSeverity severity;

  @Column(nullable = false, updatable = false, length = 100)
  private String code;

  @Column(nullable = false, updatable = false, length = 500)
  private String message;

  @Column(name = "source_node_id", updatable = false, length = 255)
  private String sourceNodeId;

  @Column(name = "page_number", updatable = false)
  private Integer pageNumber;

  @Column(updatable = false, length = 500)
  private String fallback;

  @Column(name = "is_blocking", nullable = false, updatable = false)
  private boolean blocking;

  @Column(name = "warning_position", nullable = false, updatable = false)
  private int position;

  protected DocumentConversionWarning() {}

  public DocumentConversionWarning(
      UUID id,
      UUID versionId,
      ConversionWarningSeverity severity,
      String code,
      String message,
      String sourceNodeId,
      Integer pageNumber,
      String fallback,
      boolean blocking,
      int position) {
    this.id = id;
    this.templateVersionId = versionId;
    this.severity = severity;
    this.code = code;
    this.message = message;
    this.sourceNodeId = sourceNodeId;
    this.pageNumber = pageNumber;
    this.fallback = fallback;
    this.blocking = blocking;
    this.position = position;
  }

  public UUID id() { return id; }
  public UUID templateVersionId() { return templateVersionId; }
  public ConversionWarningSeverity severity() { return severity; }
  public String code() { return code; }
  public String message() { return message; }
  public String sourceNodeId() { return sourceNodeId; }
  public Integer pageNumber() { return pageNumber; }
  public String fallback() { return fallback; }
  public boolean blocking() { return blocking; }
  public int position() { return position; }
}
