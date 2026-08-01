package com.docmind.api.extraction.application;

import static com.docmind.api.shared.web.RequestHashSupport.sha256;

import com.docmind.api.audit.application.AuditRecorder;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.extraction.api.AcceptedExtractionJobResponse;
import com.docmind.api.extraction.api.CreateExtractionRequest;
import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.domain.ExtractionRun;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import com.docmind.api.extraction.infrastructure.ExtractionRunRepository;
import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.schema.domain.ExtractionSchemaVersion;
import com.docmind.api.schema.domain.SchemaVersionStatus;
import com.docmind.api.schema.infrastructure.ExtractionSchemaVersionRepository;
import com.docmind.api.sensitive.domain.SensitiveRuleTemplateVersionStatus;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleTemplateVersionRepository;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.source.domain.SourceVersion;
import com.docmind.api.source.domain.SourceVersionStatus;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {

  private static final int DEFAULT_MAX_ATTEMPTS = 3;

  private final SourceVersionRepository sourceVersions;
  private final ExtractionSchemaVersionRepository schemaVersions;
  private final SensitiveRuleTemplateVersionRepository sensitiveRuleVersions;
  private final AsyncJobRepository jobs;
  private final ExtractionRunRepository runs;
  private final WorkspaceAccessService access;
  private final AuditRecorder audit;
  private final ObjectMapper objectMapper;

  public ExtractionService(
      SourceVersionRepository sourceVersions,
      ExtractionSchemaVersionRepository schemaVersions,
      SensitiveRuleTemplateVersionRepository sensitiveRuleVersions,
      AsyncJobRepository jobs,
      ExtractionRunRepository runs,
      WorkspaceAccessService access,
      AuditRecorder audit,
      ObjectMapper objectMapper) {
    this.sourceVersions = sourceVersions;
    this.schemaVersions = schemaVersions;
    this.sensitiveRuleVersions = sensitiveRuleVersions;
    this.jobs = jobs;
    this.runs = runs;
    this.access = access;
    this.audit = audit;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public AcceptedExtractionJobResponse create(
      UUID userId,
      UUID sourceVersionId,
      CreateExtractionRequest request,
      String idempotencyKey,
      UUID requestId) {
    SourceVersion source = sourceVersions.findById(sourceVersionId).orElseThrow(this::notFound);
    access.require(userId, source.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    requireExtractable(source);
    validateIdempotencyKey(idempotencyKey);
    String requestHash =
        sha256(
            objectMapper,
            new CreationFingerprint(
                sourceVersionId,
                request.schemaVersionId(),
                request.sensitiveRuleTemplateVersionId()));

    ExtractionRun replay =
        runs
            .findBySourceVersionIdAndCreatedByAndCreationIdempotencyKey(
                sourceVersionId, userId, idempotencyKey)
            .orElse(null);
    if (replay != null) {
      requireSameHash(replay.creationRequestHash(), requestHash);
      return new AcceptedExtractionJobResponse(replay.jobId(), replay.id(), requestId);
    }

    requireSchemaVersion(request.schemaVersionId(), source.workspaceId());
    requireSensitiveRuleVersion(request.sensitiveRuleTemplateVersionId(), source.workspaceId());

    Instant now = Instant.now();
    UUID extractionId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    jobs.save(
        new AsyncJob(
            jobId,
            source.workspaceId(),
            AsyncJobType.EXTRACTION,
            "extraction_run",
            extractionId,
            DEFAULT_MAX_ATTEMPTS,
            requestId,
            userId,
            now));
    ExtractionRun run =
        runs.saveAndFlush(
            new ExtractionRun(
                extractionId,
                jobId,
                source.workspaceId(),
                sourceVersionId,
                request.schemaVersionId(),
                request.sensitiveRuleTemplateVersionId(),
                userId,
                idempotencyKey,
                requestHash,
                objectMapper.createArrayNode(),
                now));
    audit.record(
        source.workspaceId(),
        userId,
        "extraction.created",
        "extraction_run",
        run.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of(
            "job_id", jobId.toString(),
            "source_version_id", sourceVersionId.toString(),
            "schema_version_id", request.schemaVersionId().toString(),
            "has_sensitive_rule_template", request.sensitiveRuleTemplateVersionId() != null));
    return new AcceptedExtractionJobResponse(jobId, run.id(), requestId);
  }

  private void requireExtractable(SourceVersion source) {
    if (source.status() != SourceVersionStatus.UPLOADED
        && source.status() != SourceVersionStatus.READY) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ApiErrorCode.VERSION_CONFLICT,
          ApiErrorCategory.CONFLICT,
          "原件版本尚未完成上传或已不可用");
    }
  }

  private ExtractionSchemaVersion requireSchemaVersion(UUID versionId, UUID workspaceId) {
    return schemaVersions
        .findByIdAndWorkspaceId(versionId, workspaceId)
        .filter(version -> version.status() != SchemaVersionStatus.DRAFT)
        .orElseThrow(this::notFound);
  }

  private void requireSensitiveRuleVersion(UUID versionId, UUID workspaceId) {
    if (versionId == null) {
      return;
    }
    sensitiveRuleVersions
        .findByIdAndWorkspaceId(versionId, workspaceId)
        .filter(candidate -> candidate.status() != SensitiveRuleTemplateVersionStatus.DRAFT)
        .orElseThrow(this::notFound);
  }

  private void validateIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          ApiErrorCode.VALIDATION_FAILED,
          ApiErrorCategory.VALIDATION,
          "Idempotency-Key 长度必须为 1 到 128 个字符");
    }
  }

  private void requireSameHash(String existingHash, String requestHash) {
    if (!existingHash.equals(requestHash)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ApiErrorCode.IDEMPOTENCY_CONFLICT,
          ApiErrorCategory.CONFLICT,
          "幂等键已用于不同的抽取请求");
    }
  }

  private ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        "原件、Schema、规则模板或抽取任务不存在");
  }

  private record CreationFingerprint(
      UUID sourceVersionId,
      UUID schemaVersionId,
      UUID sensitiveRuleTemplateVersionId) {}
}
