package com.docmind.api.template.application;

import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.source.domain.SourcePreview;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.domain.SourceVersionStatus;
import com.docmind.api.source.infrastructure.SourcePreviewRepository;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.docmind.api.template.domain.DocumentTemplate;
import com.docmind.api.template.infrastructure.DocumentTemplateRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateConversionStateService {
  private final DocumentTemplateRepository templates;
  private final SourceVersionRepository sources;
  private final SourcePreviewRepository previews;
  private final Clock clock;

  public TemplateConversionStateService(
      DocumentTemplateRepository templates,
      SourceVersionRepository sources,
      SourcePreviewRepository previews,
      Clock clock) {
    this.templates = templates;
    this.sources = sources;
    this.previews = previews;
    this.clock = clock;
  }

  @Transactional
  public Optional<TemplateConversionWorkItem> start(AsyncJobCommand command) {
    DocumentTemplate template =
        templates.findLockedById(command.aggregateId()).orElseThrow(() -> invalid("TEMPLATE_NOT_FOUND"));
    if (!"document_template".equals(command.aggregateType())
        || !template.id().equals(command.aggregateId())
        || !template.conversionJobId().equals(command.jobId())
        || !template.workspaceId().equals(command.workspaceId())) {
      throw invalid("TEMPLATE_JOB_IDENTITY_MISMATCH");
    }
    if (!template.start()) return Optional.empty();
    SourceVersion source =
        sources.findById(template.sourceVersionId()).orElseThrow(() -> invalid("SOURCE_VERSION_NOT_FOUND"));
    if (!source.workspaceId().equals(template.workspaceId())
        || (source.status() != SourceVersionStatus.UPLOADED
            && source.status() != SourceVersionStatus.READY)) {
      throw invalid("SOURCE_VERSION_NOT_READY");
    }
    SourcePreview preview =
        previews.findBySourceVersionId(source.id()).orElseThrow(() -> invalid("SOURCE_PREVIEW_NOT_FOUND"));
    preview.start(clock.instant());
    return Optional.of(new TemplateConversionWorkItem(template, source, preview));
  }

  @Transactional
  public void markRetrying(UUID templateId, String failureCode) {
    templates.findLockedById(templateId)
        .ifPresent(
            template -> template.retry(failureCode, template.createdBy(), clock.instant()));
  }

  @Transactional
  public void markFailed(UUID templateId, String failureCode) {
    templates.findLockedById(templateId)
        .ifPresent(template -> template.fail(failureCode, template.createdBy(), clock.instant()));
    templates.findById(templateId)
        .flatMap(template -> previews.findBySourceVersionId(template.sourceVersionId()))
        .ifPresent(preview -> preview.fail(failureCode, clock.instant()));
  }

  private TemplateConversionException invalid(String code) {
    return new TemplateConversionException(code, false);
  }
}
