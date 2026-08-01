package com.docmind.api.source.application;

import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.docmind.api.template.application.TemplateConversionException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourcePreviewPersistenceService {
  private final SourcePreviewRepository previews;
  private final SourceVersionRepository sources;
  private final Clock clock;

  public SourcePreviewPersistenceService(
      SourcePreviewRepository previews, SourceVersionRepository sources, Clock clock) {
    this.previews = previews;
    this.sources = sources;
    this.clock = clock;
  }

  @Transactional
  public void complete(
      SourcePreviewWorkItem item, String bucket, String key, Integer pageCount) {
    var preview =
        previews.findLockedById(item.preview().id()).orElseThrow(() -> invalid("SOURCE_PREVIEW_NOT_FOUND"));
    var source =
        sources.findLockedById(item.source().id()).orElseThrow(() -> invalid("SOURCE_VERSION_NOT_FOUND"));
    var now = clock.instant();
    preview.complete(bucket, key, pageCount, now);
    source.markReady(now);
  }

  private TemplateConversionException invalid(String code) {
    return new TemplateConversionException(code, false);
  }
}
