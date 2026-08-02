package com.docmind.api.source.application;

import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.source.domain.SourcePreview;
import com.docmind.api.source.domain.SourcePreviewStatus;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.domain.SourceVersionStatus;
import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.docmind.api.template.application.TemplateConversionException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourcePreviewStateService {
  private final SourcePreviewRepository previews;
  private final SourceVersionRepository sources;
  private final Clock clock;

  public SourcePreviewStateService(
      SourcePreviewRepository previews, SourceVersionRepository sources, Clock clock) {
    this.previews = previews;
    this.sources = sources;
    this.clock = clock;
  }

  @Transactional
  public Optional<SourcePreviewWorkItem> start(AsyncJobCommand command) {
    SourcePreview preview =
        previews.findLockedById(command.aggregateId()).orElseThrow(() -> invalid("SOURCE_PREVIEW_NOT_FOUND"));
    if (!"source_preview".equals(command.aggregateType())
        || !preview.id().equals(command.aggregateId())) {
      throw invalid("SOURCE_PREVIEW_JOB_IDENTITY_MISMATCH");
    }
    if (preview.status() == SourcePreviewStatus.READY) return Optional.empty();
    SourceVersion source =
        sources.findLockedById(preview.sourceVersionId()).orElseThrow(() -> invalid("SOURCE_VERSION_NOT_FOUND"));
    if (!source.workspaceId().equals(command.workspaceId())
        || (source.status() != SourceVersionStatus.UPLOADED
            && source.status() != SourceVersionStatus.READY)) {
      throw invalid("SOURCE_VERSION_NOT_READY");
    }
    preview.start(clock.instant());
    return Optional.of(new SourcePreviewWorkItem(preview, source));
  }

  @Transactional
  public void markFailed(UUID previewId, String failureCode) {
    previews.findLockedById(previewId).ifPresent(preview -> preview.fail(failureCode, clock.instant()));
  }

  private TemplateConversionException invalid(String code) {
    return new TemplateConversionException(code, false);
  }
}
