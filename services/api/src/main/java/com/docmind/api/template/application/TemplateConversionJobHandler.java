package com.docmind.api.template.application;

import com.docmind.api.extraction.ai.AiServiceClient;
import com.docmind.api.extraction.ai.AiServiceClientException;
import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.extraction.messaging.AsyncJobExecutionException;
import com.docmind.api.extraction.messaging.AsyncJobHandler;
import com.docmind.api.infrastructure.storage.DocmindStorageProperties;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.infrastructure.storage.ObjectStorageException;
import com.docmind.api.template.application.DocumentPreviewConverter.PreviewPdf;
import com.docmind.api.template.application.TemplateConversionPersistenceService.StoredResource;
import com.docmind.api.template.domain.TemplateResourceKind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docmind.ai.enabled", havingValue = "true", matchIfMissing = true)
public class TemplateConversionJobHandler implements AsyncJobHandler {
  private static final long MAX_SOURCE_BYTES = 50L * 1024 * 1024;
  private static final long MAX_RESOURCE_BYTES = 20L * 1024 * 1024;

  private final TemplateConversionStateService states;
  private final ObjectStorage storage;
  private final DocmindStorageProperties storageProperties;
  private final DocumentPreviewConverter previewConverter;
  private final AiServiceClient ai;
  private final ControlledDocumentHtmlRenderer renderer;
  private final TemplateConversionPersistenceService persistence;
  private final ObjectMapper objectMapper;

  public TemplateConversionJobHandler(
      TemplateConversionStateService states,
      ObjectStorage storage,
      DocmindStorageProperties storageProperties,
      DocumentPreviewConverter previewConverter,
      AiServiceClient ai,
      ControlledDocumentHtmlRenderer renderer,
      TemplateConversionPersistenceService persistence,
      ObjectMapper objectMapper) {
    this.states = states;
    this.storage = storage;
    this.storageProperties = storageProperties;
    this.previewConverter = previewConverter;
    this.ai = ai;
    this.renderer = renderer;
    this.persistence = persistence;
    this.objectMapper = objectMapper;
  }

  @Override
  public AsyncJobType jobType() {
    return AsyncJobType.TEMPLATE_CONVERSION;
  }

  @Override
  public void handle(AsyncJobCommand command) {
    try {
      Optional<TemplateConversionWorkItem> selected = states.start(command);
      if (selected.isEmpty()) return;
      TemplateConversionWorkItem item = selected.orElseThrow();
      byte[] source = readSource(item);
      PreviewPdf preview = previewConverter.convert(item.source().fileType(), source);
      String previewKey =
          "previews/"
              + item.template().workspaceId()
              + "/"
              + item.source().id()
              + "/preview.pdf";
      storage.put(
          storageProperties.buckets().previews(),
          previewKey,
          preview.bytes(),
          "application/pdf");
      ParseDocumentResponse parsed =
          ai.parse(
              item.source().id(),
              item.source().fileType().wireValue(),
              "und",
              item.source().originalFileName(),
              source,
              command.requestId());
      requireParsedIdentity(item, parsed);
      var rendered = renderer.render(parsed.document());
      UUID versionId = UUID.randomUUID();
      List<JsonNode> warnings = new ArrayList<>(parsed.warnings());
      List<StoredResource> resources = storeResources(item, versionId, parsed.resources(), warnings);
      persistence.persist(
          item,
          versionId,
          parsed,
          rendered,
          resources,
          warnings,
          storageProperties.buckets().previews(),
          previewKey,
          preview.pageCount());
    } catch (TemplateConversionException exception) {
      throw new AsyncJobExecutionException(
          exception.failureCode(), exception.retryable(), exception);
    } catch (AiServiceClientException exception) {
      throw new AsyncJobExecutionException(
          exception.failureCode(), exception.retryable(), exception);
    } catch (ObjectStorageException exception) {
      throw new AsyncJobExecutionException("TEMPLATE_STORAGE_UNAVAILABLE", true, exception);
    } catch (IOException exception) {
      throw new AsyncJobExecutionException("SOURCE_READ_FAILED", true, exception);
    }
  }

  @Override
  public void onRetryScheduled(AsyncJobCommand command, String failureCode) {
    states.markRetrying(command.aggregateId(), failureCode);
  }

  @Override
  public void onTerminalFailure(AsyncJobCommand command, String failureCode) {
    states.markFailed(command.aggregateId(), failureCode);
  }

  private byte[] readSource(TemplateConversionWorkItem item) throws IOException {
    Long size = item.source().sizeBytes();
    if (size == null || size <= 0 || size > MAX_SOURCE_BYTES) {
      throw new TemplateConversionException("SOURCE_SIZE_INVALID", false);
    }
    try (InputStream input = storage.open(item.source().objectBucket(), item.source().objectKey())) {
      byte[] bytes = input.readNBytes(Math.toIntExact(size + 1));
      if (bytes.length != size.intValue()) {
        throw new TemplateConversionException("SOURCE_SIZE_MISMATCH", false);
      }
      if (item.source().sha256() == null
          || !sha256(bytes).equalsIgnoreCase(item.source().sha256())) {
        throw new TemplateConversionException("SOURCE_DIGEST_MISMATCH", false);
      }
      return bytes;
    }
  }

  private void requireParsedIdentity(
      TemplateConversionWorkItem item, ParseDocumentResponse parsed) {
    if (parsed == null
        || !item.source().id().equals(parsed.sourceVersionId())
        || !item.source().fileType().wireValue().equals(parsed.sourceFormat())
        || parsed.document() == null
        || parsed.resources() == null
        || parsed.warnings() == null
        || parsed.parserVersion() == null
        || parsed.parserVersion().isBlank()) {
      throw new TemplateConversionException("PARSED_DOCUMENT_IDENTITY_INVALID", false);
    }
  }

  private List<StoredResource> storeResources(
      TemplateConversionWorkItem item,
      UUID versionId,
      List<JsonNode> parsedResources,
      List<JsonNode> warnings) {
    if (parsedResources.size() > 10_000) {
      throw new TemplateConversionException("TEMPLATE_RESOURCE_LIMIT", false);
    }
    long total = 0;
    List<StoredResource> result = new ArrayList<>();
    for (JsonNode resource : parsedResources) {
      String encoded = resource.path("content_base64").asText(null);
      if (encoded == null) {
        warnings.add(resourceWarning("RESOURCE_CONTENT_UNAVAILABLE", resource.path("id").asText()));
        continue;
      }
      byte[] bytes;
      try {
        bytes = Base64.getDecoder().decode(encoded);
      } catch (IllegalArgumentException exception) {
        throw new TemplateConversionException("TEMPLATE_RESOURCE_INVALID", false, exception);
      }
      total += bytes.length;
      if (total > MAX_RESOURCE_BYTES
          || resource.path("byte_length").asLong(-1) != bytes.length
          || !sha256(bytes).equals(resource.path("sha256").asText())) {
        throw new TemplateConversionException("TEMPLATE_RESOURCE_INVALID", false);
      }
      UUID id;
      try {
        id = UUID.fromString(resource.path("id").asText());
      } catch (IllegalArgumentException exception) {
        throw new TemplateConversionException("TEMPLATE_RESOURCE_INVALID", false, exception);
      }
      String mediaType = safeMediaType(resource.path("media_type").asText());
      String filename = safeFilename(resource.path("filename").asText("resource.bin"));
      String key =
          "resources/"
              + item.template().workspaceId()
              + "/"
              + item.template().id()
              + "/"
              + versionId
              + "/"
              + id
              + "/"
              + filename;
      storage.put(storageProperties.buckets().templates(), key, bytes, mediaType);
      result.add(
          new StoredResource(
              id,
              mediaType.startsWith("image/")
                  ? TemplateResourceKind.IMAGE
                  : TemplateResourceKind.ATTACHMENT,
              mediaType,
              bytes.length,
              sha256(bytes),
              storageProperties.buckets().templates(),
              key,
              filename));
    }
    return List.copyOf(result);
  }

  private JsonNode resourceWarning(String code, String resourceId) {
    var warning = objectMapper.createObjectNode();
    warning.put("code", code);
    warning.put("severity", "warning");
    warning.put("message", "资源未包含可保存内容，模板使用缺失资源占位");
    warning.put("node_id", resourceId);
    return warning;
  }

  private String safeMediaType(String value) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
    if (!normalized.matches("^[a-z0-9][a-z0-9.+-]{0,63}/[a-z0-9][a-z0-9.+-]{0,127}$")) {
      throw new TemplateConversionException("TEMPLATE_RESOURCE_MEDIA_TYPE_INVALID", false);
    }
    return normalized;
  }

  private String safeFilename(String value) {
    String normalized = value == null ? "resource.bin" : value.replace('\\', '/');
    normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
    normalized = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
    if (normalized.isBlank()) normalized = "resource.bin";
    return normalized.substring(0, Math.min(200, normalized.length()));
  }

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
