package com.docmind.api.extraction.application;

import com.docmind.api.extraction.domain.ExtractionRun;
import com.docmind.api.extraction.infrastructure.ExtractionRunRepository;
import com.docmind.api.extraction.messaging.AsyncJobCommand;
import com.docmind.api.schema.domain.ExtractionSchemaVersion;
import com.docmind.api.schema.infrastructure.ExtractionSchemaFieldRepository;
import com.docmind.api.schema.infrastructure.ExtractionSchemaVersionRepository;
import com.docmind.api.sensitive.domain.SensitiveRule;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleRepository;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.domain.SourceVersionStatus;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionExecutionStateService {

  private final ExtractionRunRepository runs;
  private final SourceVersionRepository sources;
  private final ExtractionSchemaVersionRepository schemaVersions;
  private final ExtractionSchemaFieldRepository fields;
  private final SensitiveRuleRepository sensitiveRules;
  private final ExtractionEventService events;
  private final Clock clock;

  @Autowired
  public ExtractionExecutionStateService(
      ExtractionRunRepository runs,
      SourceVersionRepository sources,
      ExtractionSchemaVersionRepository schemaVersions,
      ExtractionSchemaFieldRepository fields,
      SensitiveRuleRepository sensitiveRules,
      ExtractionEventService events) {
    this(
        runs,
        sources,
        schemaVersions,
        fields,
        sensitiveRules,
        events,
        Clock.systemUTC());
  }

  ExtractionExecutionStateService(
      ExtractionRunRepository runs,
      SourceVersionRepository sources,
      ExtractionSchemaVersionRepository schemaVersions,
      ExtractionSchemaFieldRepository fields,
      SensitiveRuleRepository sensitiveRules,
      ExtractionEventService events,
      Clock clock) {
    this.runs = runs;
    this.sources = sources;
    this.schemaVersions = schemaVersions;
    this.fields = fields;
    this.sensitiveRules = sensitiveRules;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public Optional<ExtractionWorkItem> start(AsyncJobCommand command) {
    ExtractionRun run =
        runs.findLockedById(command.aggregateId())
            .orElseThrow(() -> new ExtractionExecutionStateException("EXTRACTION_RUN_NOT_FOUND"));
    requireCommandMatches(command, run);
    if (!run.start()) {
      return Optional.empty();
    }
    events.publishAfterCommit(run);
    SourceVersion source =
        sources
            .findById(run.sourceVersionId())
            .orElseThrow(() -> new ExtractionExecutionStateException("SOURCE_VERSION_NOT_FOUND"));
    if (!source.workspaceId().equals(run.workspaceId())
        || (source.status() != SourceVersionStatus.UPLOADED
            && source.status() != SourceVersionStatus.READY)) {
      throw new ExtractionExecutionStateException("SOURCE_VERSION_NOT_READY");
    }
    ExtractionSchemaVersion schemaVersion =
        schemaVersions
            .findByIdAndWorkspaceId(run.schemaVersionId(), run.workspaceId())
            .orElseThrow(() -> new ExtractionExecutionStateException("SCHEMA_VERSION_NOT_FOUND"));
    List<com.docmind.api.schema.domain.ExtractionSchemaField> schemaFields =
        fields.findAllBySchemaVersionIdOrderByPositionAsc(schemaVersion.id());
    if (schemaFields.isEmpty()) {
      throw new ExtractionExecutionStateException("SCHEMA_FIELDS_MISSING");
    }
    List<SensitiveRule> rules =
        run.sensitiveRuleTemplateVersionId() == null
            ? List.of()
            : sensitiveRules.findAllByTemplateVersionIdOrderByPositionAsc(
                run.sensitiveRuleTemplateVersionId());
    return Optional.of(new ExtractionWorkItem(run, source, schemaVersion, schemaFields, rules));
  }

  @Transactional
  public void markRetrying(UUID runId, String failureCode) {
    runs.findLockedById(runId)
        .ifPresent(
            run -> {
              run.markRetrying(failureCode);
              events.publishAfterCommit(run);
            });
  }

  @Transactional
  public void markFailed(UUID runId, String failureCode) {
    runs.findLockedById(runId)
        .ifPresent(
            run -> {
              run.markFailed(failureCode, clock.instant());
              events.publishAfterCommit(run);
            });
  }

  private void requireCommandMatches(AsyncJobCommand command, ExtractionRun run) {
    if (!"extraction_run".equals(command.aggregateType())
        || !run.id().equals(command.aggregateId())
        || !run.jobId().equals(command.jobId())
        || !run.workspaceId().equals(command.workspaceId())) {
      throw new ExtractionExecutionStateException("EXTRACTION_JOB_IDENTITY_MISMATCH");
    }
  }
}
