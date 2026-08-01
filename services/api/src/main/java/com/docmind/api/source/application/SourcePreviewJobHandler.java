package com.docmind.api.source.application;

import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.extraction.messaging.AsyncJobExecutionException;
import com.docmind.api.extraction.messaging.AsyncJobHandler;
import com.docmind.api.infrastructure.storage.DocmindStorageProperties;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.infrastructure.storage.ObjectStorageException;
import com.docmind.api.template.application.DocumentPreviewConverter;
import com.docmind.api.template.application.DocumentPreviewConverter.PreviewPdf;
import com.docmind.api.template.application.TemplateConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SourcePreviewJobHandler implements AsyncJobHandler {
  private static final long MAX_SOURCE_BYTES = 10L * 1024 * 1024;

  private final SourcePreviewStateService states;
  private final SourcePreviewPersistenceService persistence;
  private final ObjectStorage storage;
  private final DocmindStorageProperties storageProperties;
  private final DocumentPreviewConverter converter;

  public SourcePreviewJobHandler(
      SourcePreviewStateService states,
      SourcePreviewPersistenceService persistence,
      ObjectStorage storage,
      DocmindStorageProperties storageProperties,
      DocumentPreviewConverter converter) {
    this.states = states;
    this.persistence = persistence;
    this.storage = storage;
    this.storageProperties = storageProperties;
    this.converter = converter;
  }

  @Override
  public AsyncJobType jobType() {
    return AsyncJobType.SOURCE_PREVIEW;
  }

  @Override
  public void handle(AsyncJobCommand command) {
    try {
      Optional<SourcePreviewWorkItem> selected = states.start(command);
      if (selected.isEmpty()) return;
      SourcePreviewWorkItem item = selected.orElseThrow();
      PreviewPdf preview = converter.convert(item.source().fileType(), readSource(item));
      String key =
          "previews/"
              + item.source().workspaceId()
              + "/"
              + item.source().id()
              + "/preview.pdf";
      String bucket = storageProperties.buckets().previews();
      storage.put(bucket, key, preview.bytes(), "application/pdf");
      persistence.complete(item, bucket, key, preview.pageCount());
    } catch (TemplateConversionException exception) {
      throw new AsyncJobExecutionException(exception.failureCode(), exception.retryable(), exception);
    } catch (ObjectStorageException exception) {
      throw new AsyncJobExecutionException("PREVIEW_STORAGE_UNAVAILABLE", true, exception);
    } catch (IOException exception) {
      throw new AsyncJobExecutionException("SOURCE_READ_FAILED", true, exception);
    }
  }

  @Override
  public void onTerminalFailure(AsyncJobCommand command, String failureCode) {
    states.markFailed(command.aggregateId(), failureCode);
  }

  private byte[] readSource(SourcePreviewWorkItem item) throws IOException {
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

  private String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
