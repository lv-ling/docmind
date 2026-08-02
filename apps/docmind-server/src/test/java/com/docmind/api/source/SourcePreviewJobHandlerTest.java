package com.docmind.api.source;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.infrastructure.storage.DocmindStorageProperties;
import com.docmind.api.infrastructure.storage.DocmindStorageProperties.Buckets;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.source.application.SourcePreviewJobHandler;
import com.docmind.api.source.application.SourcePreviewPersistenceService;
import com.docmind.api.source.application.SourcePreviewStateService;
import com.docmind.api.source.application.SourcePreviewWorkItem;
import com.docmind.api.source.domain.SourceFileType;
import com.docmind.api.source.domain.SourcePreview;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.template.application.DocumentPreviewConverter;
import com.docmind.api.template.application.DocumentPreviewConverter.PreviewPdf;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SourcePreviewJobHandlerTest {

  @Test
  void convertsAnImmutableSourceAndPersistsTheProtectedPreview() throws Exception {
    byte[] sourceBytes = "source-docx".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] previewBytes = "%PDF-preview".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    Instant now = Instant.parse("2026-08-01T00:00:00Z");
    UUID workspaceId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    SourceVersion source =
        new SourceVersion(
            sourceId,
            UUID.randomUUID(),
            workspaceId,
            1,
            "sample.docx",
            SourceFileType.DOCX,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            sourceBytes.length,
            "sources",
            "staging/sample.docx",
            "sources",
            "immutable/sample.docx",
            UUID.randomUUID(),
            now);
    source.complete(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        sourceBytes.length,
        sha256(sourceBytes),
        "etag",
        now);
    SourcePreview preview = new SourcePreview(UUID.randomUUID(), sourceId, now);
    SourcePreviewWorkItem item = new SourcePreviewWorkItem(preview, source);

    SourcePreviewStateService states = mock(SourcePreviewStateService.class);
    SourcePreviewPersistenceService persistence = mock(SourcePreviewPersistenceService.class);
    ObjectStorage storage = mock(ObjectStorage.class);
    DocumentPreviewConverter converter = mock(DocumentPreviewConverter.class);
    DocmindStorageProperties properties =
        new DocmindStorageProperties(
            "http://localhost:9000",
            "access",
            "secret",
            Duration.ofMinutes(15),
            new Buckets("sources", "previews", "templates", "exports"));
    AsyncJobCommand command =
        new AsyncJobCommand(
            AsyncJobCommand.CURRENT_SCHEMA_VERSION,
            UUID.randomUUID(),
            UUID.randomUUID(),
            workspaceId,
            AsyncJobType.SOURCE_PREVIEW,
            "source_preview",
            preview.id(),
            1,
            UUID.randomUUID(),
            now);
    when(states.start(command)).thenReturn(Optional.of(item));
    when(storage.open("sources", "immutable/sample.docx"))
        .thenReturn(new ByteArrayInputStream(sourceBytes));
    when(converter.convert(SourceFileType.DOCX, sourceBytes))
        .thenReturn(new PreviewPdf(previewBytes, 2));

    SourcePreviewJobHandler handler =
        new SourcePreviewJobHandler(states, persistence, storage, properties, converter);
    handler.handle(command);

    String expectedKey = "previews/" + workspaceId + "/" + sourceId + "/preview.pdf";
    verify(storage).put("previews", expectedKey, previewBytes, "application/pdf");
    verify(persistence).complete(item, "previews", expectedKey, 2);
    org.assertj.core.api.Assertions.assertThat(handler.jobType())
        .isEqualTo(AsyncJobType.SOURCE_PREVIEW);
  }

  private String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
