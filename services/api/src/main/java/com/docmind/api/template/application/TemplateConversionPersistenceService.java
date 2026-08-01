package com.docmind.api.template.application;

import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.infrastructure.crypto.JsonEnvelopeEncryption;
import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.template.domain.ConversionWarningSeverity;
import com.docmind.api.template.domain.DocumentConversionWarning;
import com.docmind.api.template.domain.DocumentTemplateResource;
import com.docmind.api.template.domain.DocumentTemplateVersion;
import com.docmind.api.template.domain.ParsedContent;
import com.docmind.api.template.domain.TemplateResourceKind;
import com.docmind.api.template.domain.TemplateVersionStatus;
import com.docmind.api.template.infrastructure.DocumentConversionWarningRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateResourceRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateVersionRepository;
import com.docmind.api.template.infrastructure.ParsedContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateConversionPersistenceService {
  private final DocumentTemplateRepository templates;
  private final DocumentTemplateVersionRepository versions;
  private final ParsedContentRepository parsedContents;
  private final DocumentTemplateResourceRepository resources;
  private final DocumentConversionWarningRepository warnings;
  private final SourcePreviewRepository previews;
  private final JsonEnvelopeEncryption encryption;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public TemplateConversionPersistenceService(
      DocumentTemplateRepository templates,
      DocumentTemplateVersionRepository versions,
      ParsedContentRepository parsedContents,
      DocumentTemplateResourceRepository resources,
      DocumentConversionWarningRepository warnings,
      SourcePreviewRepository previews,
      JsonEnvelopeEncryption encryption,
      ObjectMapper objectMapper,
      Clock clock) {
    this.templates = templates;
    this.versions = versions;
    this.parsedContents = parsedContents;
    this.resources = resources;
    this.warnings = warnings;
    this.previews = previews;
    this.encryption = encryption;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public void persist(
      TemplateConversionWorkItem item,
      UUID versionId,
      ParseDocumentResponse parsed,
      ControlledDocumentHtmlRenderer.RenderedDocument rendered,
      List<StoredResource> storedResources,
      List<JsonNode> conversionWarnings,
      String previewBucket,
      String previewKey,
      Integer pageCount) {
    var template = templates.findLockedById(item.template().id()).orElseThrow(() -> invalid("TEMPLATE_NOT_FOUND"));
    if (!template.conversionStatus().name().equals("RUNNING")) throw invalid("TEMPLATE_NOT_RUNNING");
    Instant now = clock.instant();
    UUID parsedContentId = UUID.randomUUID();
    parsedContents.save(
        new ParsedContent(
            parsedContentId,
            item.source().id(),
            parserName(parsed.parserVersion()),
            parsed.parserVersion(),
            encryption.encrypt(parsed.document(), "parsed-content:" + parsedContentId),
            now));
    var emptyDiff = objectMapper.createObjectNode();
    emptyDiff.put("version", "1.0");
    emptyDiff.put("truncated", false);
    emptyDiff.putArray("changes");
    versions.saveAndFlush(
        new DocumentTemplateVersion(
            versionId,
            template.id(),
            template.workspaceId(),
            item.source().id(),
            parsedContentId,
            versionId,
            1,
            TemplateVersionStatus.GENERATED,
            encryption.encrypt(parsed.document(), "template-model:" + versionId),
            encryption.encrypt(
                objectMapper.getNodeFactory().textNode(rendered.html()), "template-html:" + versionId),
            rendered.css(),
            rendered.policyVersion(),
            "由原件自动生成",
            encryption.encrypt(emptyDiff, "template-diff:" + versionId),
            item.template().createdBy(),
            now));
    storedResources.forEach(
        resource ->
            resources.save(
                new DocumentTemplateResource(
                    resource.id(),
                    versionId,
                    resource.kind(),
                    resource.contentType(),
                    resource.bytes(),
                    resource.sha256(),
                    resource.bucket(),
                    resource.key(),
                    resource.filename(),
                    now)));
    for (int position = 0; position < conversionWarnings.size(); position++) {
      JsonNode warning = conversionWarnings.get(position);
      ConversionWarningSeverity severity = severity(warning.path("severity").asText());
      warnings.save(
          new DocumentConversionWarning(
              UUID.randomUUID(),
              versionId,
              severity,
              safe(warning.path("code").asText("CONVERSION_WARNING"), 100),
              safe(warning.path("message").asText("存在未完整支持的版式"), 500),
              nullableSafe(warning.path("node_id").asText(null), 255),
              warning.path("page_number").isInt() ? warning.path("page_number").intValue() : null,
              severity == ConversionWarningSeverity.ERROR ? "已使用安全回退版式" : null,
              severity == ConversionWarningSeverity.ERROR,
              position));
    }
    previews.findById(item.preview().id()).orElseThrow(() -> invalid("SOURCE_PREVIEW_NOT_FOUND"))
        .complete(previewBucket, previewKey, pageCount, now);
    template.complete(versionId, item.template().createdBy(), now);
  }

  private String parserName(String parserVersion) {
    int separator = parserVersion.indexOf('/');
    return separator <= 0 ? parserVersion : parserVersion.substring(0, separator);
  }

  private ConversionWarningSeverity severity(String value) {
    try {
      return ConversionWarningSeverity.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return ConversionWarningSeverity.WARNING;
    }
  }

  private String safe(String value, int limit) {
    String normalized = value == null ? "" : value.strip();
    return normalized.isEmpty() ? "CONVERSION_WARNING" : normalized.substring(0, Math.min(limit, normalized.length()));
  }

  private String nullableSafe(String value, int limit) {
    return value == null || value.isBlank() ? null : value.substring(0, Math.min(limit, value.length()));
  }

  private TemplateConversionException invalid(String code) {
    return new TemplateConversionException(code, false);
  }

  public record StoredResource(
      UUID id,
      TemplateResourceKind kind,
      String contentType,
      long bytes,
      String sha256,
      String bucket,
      String key,
      String filename) {}
}
