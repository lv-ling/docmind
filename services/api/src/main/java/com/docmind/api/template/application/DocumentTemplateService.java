package com.docmind.api.template.application;

import static com.docmind.api.shared.web.RequestHashSupport.sha256;

import com.docmind.api.audit.application.AuditRecorder;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.extraction.domain.AsyncJob;
import com.docmind.api.extraction.domain.AsyncJobType;
import com.docmind.api.extraction.infrastructure.AsyncJobRepository;
import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.infrastructure.crypto.JsonEnvelopeEncryption;
import com.docmind.api.infrastructure.storage.ObjectStorage;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.source.domain.SourceVersionStatus;
import com.docmind.api.source.infrastructure.SourceVersionRepository;
import com.docmind.api.template.api.AcceptedTemplateJobResponse;
import com.docmind.api.template.api.ConversionWarningResponse;
import com.docmind.api.template.api.CreateTemplateRequest;
import com.docmind.api.template.api.CreateTemplateVersionRequest;
import com.docmind.api.template.api.DocumentTemplateDetailResponse;
import com.docmind.api.template.api.DocumentTemplateResponse;
import com.docmind.api.template.api.DocumentTemplateVersionResponse;
import com.docmind.api.template.api.PublishTemplateVersionRequest;
import com.docmind.api.template.api.RollbackTemplateRequest;
import com.docmind.api.template.api.SafeHtmlDocumentResponse;
import com.docmind.api.template.api.TemplateResourceResponse;
import com.docmind.api.template.domain.DocumentConversionWarning;
import com.docmind.api.template.domain.DocumentTemplate;
import com.docmind.api.template.domain.DocumentTemplateOperation;
import com.docmind.api.template.domain.DocumentTemplateOperationType;
import com.docmind.api.template.domain.DocumentTemplateVersion;
import com.docmind.api.template.domain.TemplateVersionStatus;
import com.docmind.api.template.infrastructure.DocumentConversionWarningRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateOperationRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateResourceRepository;
import com.docmind.api.template.infrastructure.DocumentTemplateVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentTemplateService {
  private static final int DEFAULT_MAX_ATTEMPTS = 3;
  private static final int MAX_RESOURCE_BYTES = 20 * 1024 * 1024;

  private final DocumentTemplateRepository templates;
  private final DocumentTemplateVersionRepository versions;
  private final DocumentTemplateResourceRepository resources;
  private final DocumentConversionWarningRepository warnings;
  private final DocumentTemplateOperationRepository operations;
  private final SourceVersionRepository sourceVersions;
  private final AsyncJobRepository jobs;
  private final WorkspaceAccessService access;
  private final ControlledDocumentValidator validator;
  private final ControlledDocumentHtmlRenderer renderer;
  private final ControlledDocumentDiff differ;
  private final JsonEnvelopeEncryption encryption;
  private final ObjectStorage storage;
  private final AuditRecorder audit;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public DocumentTemplateService(
      DocumentTemplateRepository templates,
      DocumentTemplateVersionRepository versions,
      DocumentTemplateResourceRepository resources,
      DocumentConversionWarningRepository warnings,
      DocumentTemplateOperationRepository operations,
      SourceVersionRepository sourceVersions,
      AsyncJobRepository jobs,
      WorkspaceAccessService access,
      ControlledDocumentValidator validator,
      ControlledDocumentHtmlRenderer renderer,
      ControlledDocumentDiff differ,
      JsonEnvelopeEncryption encryption,
      ObjectStorage storage,
      AuditRecorder audit,
      ObjectMapper objectMapper,
      Clock clock) {
    this.templates = templates;
    this.versions = versions;
    this.resources = resources;
    this.warnings = warnings;
    this.operations = operations;
    this.sourceVersions = sourceVersions;
    this.jobs = jobs;
    this.access = access;
    this.validator = validator;
    this.renderer = renderer;
    this.differ = differ;
    this.encryption = encryption;
    this.storage = storage;
    this.audit = audit;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public AcceptedTemplateJobResponse create(
      UUID userId,
      UUID sourceVersionId,
      CreateTemplateRequest request,
      String idempotencyKey,
      UUID requestId) {
    var source = sourceVersions.findById(sourceVersionId).orElseThrow(this::notFound);
    access.require(userId, source.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    if (source.status() != SourceVersionStatus.UPLOADED
        && source.status() != SourceVersionStatus.READY) throw conflict("原件尚未完成上传校验");
    validateKey(idempotencyKey);
    String name = request.name().strip();
    String requestHash = sha256(objectMapper, new CreateFingerprint(sourceVersionId, name));
    DocumentTemplate replay =
        templates
            .findBySourceVersionIdAndCreatedByAndCreationIdempotencyKey(
                sourceVersionId, userId, idempotencyKey)
            .orElse(null);
    if (replay != null) {
      if (!replay.creationRequestHash().equals(requestHash)) throw idempotencyConflict();
      return new AcceptedTemplateJobResponse(replay.conversionJobId(), replay.id(), requestId);
    }
    Instant now = clock.instant();
    UUID templateId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    jobs.save(
        new AsyncJob(
            jobId,
            source.workspaceId(),
            AsyncJobType.TEMPLATE_CONVERSION,
            "document_template",
            templateId,
            DEFAULT_MAX_ATTEMPTS,
            requestId,
            userId,
            now));
    templates.saveAndFlush(
        new DocumentTemplate(
            templateId,
            source.workspaceId(),
            source.sourceDocumentId(),
            source.id(),
            jobId,
            name,
            userId,
            idempotencyKey,
            requestHash,
            now));
    audit.record(
        source.workspaceId(),
        userId,
        "template.conversion_created",
        "document_template",
        templateId,
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("source_version_id", sourceVersionId.toString(), "job_id", jobId.toString()));
    return new AcceptedTemplateJobResponse(jobId, templateId, requestId);
  }

  @Transactional(readOnly = true)
  public List<DocumentTemplateResponse> list(UUID userId, UUID workspaceId) {
    access.require(userId, workspaceId, WorkspacePermission.VIEW_WORKSPACE);
    return templates.findAllByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).stream()
        .map(DocumentTemplateResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public DocumentTemplateDetailResponse get(UUID userId, UUID templateId) {
    DocumentTemplate template = templates.findById(templateId).orElseThrow(this::notFound);
    access.require(userId, template.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    return detail(template);
  }

  @Transactional
  public DocumentTemplateVersionResponse createVersion(
      UUID userId,
      UUID templateId,
      CreateTemplateVersionRequest request,
      String idempotencyKey,
      UUID requestId) {
    DocumentTemplate template = templates.findLockedById(templateId).orElseThrow(this::notFound);
    access.require(userId, template.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    validateKey(idempotencyKey);
    String requestHash =
        sha256(
            objectMapper,
            new VersionFingerprint(
                templateId,
                request.baseVersionId(),
                request.documentModel(),
                request.changeSummary()));
    DocumentTemplateOperation replay =
        findOperation(userId, DocumentTemplateOperationType.CREATE_VERSION, idempotencyKey);
    if (replay != null) return replayVersion(replay, templateId, requestHash);
    if (!request.baseVersionId().equals(template.currentVersionId())) {
      throw conflict("模板已产生更新版本，请重新载入后再保存");
    }
    DocumentTemplateVersion base =
        versions.findByIdAndTemplateId(request.baseVersionId(), templateId).orElseThrow(this::notFound);
    validator.validate(request.documentModel());
    var rendered = renderer.render(request.documentModel());
    JsonNode before = decryptModel(base);
    UUID versionId = UUID.randomUUID();
    Instant now = clock.instant();
    int number = nextVersionNumber(templateId);
    DocumentTemplateVersion created =
        versions.saveAndFlush(
            new DocumentTemplateVersion(
                versionId,
                templateId,
                template.workspaceId(),
                base.sourceVersionId(),
                base.parsedContentId(),
                base.resourceVersionId(),
                number,
                TemplateVersionStatus.CHECKING,
                encryption.encrypt(request.documentModel(), "template-model:" + versionId),
                encryption.encrypt(
                    objectMapper.getNodeFactory().textNode(rendered.html()),
                    "template-html:" + versionId),
                rendered.css(),
                rendered.policyVersion(),
                request.changeSummary().strip(),
                encryption.encrypt(
                    differ.compare(before, request.documentModel()),
                    "template-diff:" + versionId),
                userId,
                now));
    cloneWarnings(base.id(), created.id());
    template.moveToVersion(created.id(), userId, now);
    saveOperation(
        templateId,
        created.id(),
        DocumentTemplateOperationType.CREATE_VERSION,
        idempotencyKey,
        requestHash,
        userId,
        now);
    audit.record(
        template.workspaceId(),
        userId,
        "template.version_created",
        "document_template_version",
        created.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("template_id", templateId.toString(), "version_number", number));
    return versionResponse(created);
  }

  @Transactional
  public DocumentTemplateVersionResponse publish(
      UUID userId,
      UUID templateId,
      UUID versionId,
      PublishTemplateVersionRequest request,
      String idempotencyKey,
      UUID requestId) {
    DocumentTemplate template = templates.findLockedById(templateId).orElseThrow(this::notFound);
    access.require(userId, template.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    validateKey(idempotencyKey);
    String requestHash = sha256(objectMapper, new PublishFingerprint(templateId, versionId, request.note()));
    DocumentTemplateOperation replay =
        findOperation(userId, DocumentTemplateOperationType.PUBLISH, idempotencyKey);
    if (replay != null) return replayVersion(replay, templateId, requestHash);
    if (!versionId.equals(template.currentVersionId())) throw conflict("只能发布当前待校验版本");
    DocumentTemplateVersion target =
        versions.findByIdAndTemplateId(versionId, templateId).orElseThrow(this::notFound);
    if (warnings.existsByTemplateVersionIdAndBlockingTrue(versionId)) {
      throw conflict("模板包含阻断级转换告警，修正后才能发布");
    }
    Instant now = clock.instant();
    versions
        .findFirstByTemplateIdAndStatusOrderByVersionNumberDesc(
            templateId, TemplateVersionStatus.PUBLISHED)
        .filter(previous -> !previous.id().equals(target.id()))
        .ifPresent(DocumentTemplateVersion::supersede);
    target.publish(userId, now);
    template.moveToVersion(target.id(), userId, now);
    saveOperation(
        templateId,
        target.id(),
        DocumentTemplateOperationType.PUBLISH,
        idempotencyKey,
        requestHash,
        userId,
        now);
    audit.record(
        template.workspaceId(),
        userId,
        "template.version_published",
        "document_template_version",
        target.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("template_id", templateId.toString(), "version_number", target.versionNumber()));
    return versionResponse(target);
  }

  @Transactional
  public DocumentTemplateVersionResponse rollback(
      UUID userId,
      UUID templateId,
      RollbackTemplateRequest request,
      String idempotencyKey,
      UUID requestId) {
    DocumentTemplate template = templates.findLockedById(templateId).orElseThrow(this::notFound);
    access.require(userId, template.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    validateKey(idempotencyKey);
    String requestHash =
        sha256(
            objectMapper,
            new RollbackFingerprint(
                templateId, request.targetVersionId(), request.changeSummary()));
    DocumentTemplateOperation replay =
        findOperation(userId, DocumentTemplateOperationType.ROLLBACK, idempotencyKey);
    if (replay != null) return replayVersion(replay, templateId, requestHash);
    DocumentTemplateVersion target =
        versions.findByIdAndTemplateId(request.targetVersionId(), templateId).orElseThrow(this::notFound);
    DocumentTemplateVersion current =
        versions.findByIdAndTemplateId(template.currentVersionId(), templateId).orElseThrow(this::notFound);
    JsonNode restored = decryptModel(target);
    var rendered = renderer.render(restored);
    UUID newId = UUID.randomUUID();
    Instant now = clock.instant();
    DocumentTemplateVersion rolledBack =
        versions.saveAndFlush(
            new DocumentTemplateVersion(
                newId,
                templateId,
                template.workspaceId(),
                target.sourceVersionId(),
                target.parsedContentId(),
                target.resourceVersionId(),
                nextVersionNumber(templateId),
                TemplateVersionStatus.CHECKING,
                encryption.encrypt(restored, "template-model:" + newId),
                encryption.encrypt(
                    objectMapper.getNodeFactory().textNode(rendered.html()), "template-html:" + newId),
                rendered.css(),
                rendered.policyVersion(),
                request.changeSummary().strip(),
                encryption.encrypt(
                    differ.compare(decryptModel(current), restored), "template-diff:" + newId),
                userId,
                now));
    cloneWarnings(target.id(), rolledBack.id());
    versions
        .findFirstByTemplateIdAndStatusOrderByVersionNumberDesc(
            templateId, TemplateVersionStatus.PUBLISHED)
        .ifPresent(DocumentTemplateVersion::supersede);
    rolledBack.publish(userId, now);
    template.moveToVersion(rolledBack.id(), userId, now);
    saveOperation(
        templateId,
        rolledBack.id(),
        DocumentTemplateOperationType.ROLLBACK,
        idempotencyKey,
        requestHash,
        userId,
        now);
    audit.record(
        template.workspaceId(),
        userId,
        "template.rolled_back",
        "document_template_version",
        rolledBack.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("target_version_id", target.id().toString()));
    return versionResponse(rolledBack);
  }

  @Transactional(readOnly = true)
  public BinaryResource getResource(UUID userId, UUID resourceId) {
    var resource = resources.findById(resourceId).orElseThrow(this::notFound);
    var version = versions.findById(resource.templateVersionId()).orElseThrow(this::notFound);
    access.require(userId, version.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    try (InputStream input = storage.open(resource.objectBucket(), resource.objectKey())) {
      byte[] bytes = input.readNBytes(MAX_RESOURCE_BYTES + 1);
      if (bytes.length > MAX_RESOURCE_BYTES) throw conflict("模板资源超过读取上限");
      return new BinaryResource(bytes, resource.contentType(), resource.originalFilename());
    } catch (IOException exception) {
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          ApiErrorCode.DEPENDENCY_UNAVAILABLE,
          ApiErrorCategory.DEPENDENCY,
          "模板资源暂时不可用");
    }
  }

  private DocumentTemplateDetailResponse detail(DocumentTemplate template) {
    List<DocumentTemplateVersionResponse> all =
        versions.findAllByTemplateIdOrderByVersionNumberDesc(template.id()).stream()
            .map(this::versionResponse)
            .toList();
    DocumentTemplateVersionResponse current =
        all.stream().filter(item -> item.id().equals(template.currentVersionId())).findFirst().orElse(null);
    return new DocumentTemplateDetailResponse(DocumentTemplateResponse.from(template), current, all);
  }

  private DocumentTemplateVersionResponse versionResponse(DocumentTemplateVersion version) {
    JsonNode model = decryptModel(version);
    String html =
        encryption.decrypt(version.htmlEnvelope(), "template-html:" + version.id()).asText();
    JsonNode diff = encryption.decrypt(version.diffEnvelope(), "template-diff:" + version.id());
    return new DocumentTemplateVersionResponse(
        version.id(),
        version.templateId(),
        version.workspaceId(),
        version.sourceVersionId(),
        version.parsedContentId(),
        version.versionNumber(),
        version.status().wireValue(),
        new SafeHtmlDocumentResponse(
            html, version.cssText(), version.sanitizationPolicyVersion()),
        model,
        resources.findAllByTemplateVersionIdOrderByOriginalFilenameAsc(version.resourceVersionId())
            .stream()
            .map(TemplateResourceResponse::from)
            .toList(),
        warnings.findAllByTemplateVersionIdOrderByPositionAsc(version.id()).stream()
            .map(ConversionWarningResponse::from)
            .toList(),
        version.changeSummary(),
        diff,
        version.createdAt(),
        version.createdBy(),
        version.publishedAt());
  }

  private JsonNode decryptModel(DocumentTemplateVersion version) {
    return encryption.decrypt(version.documentModelEnvelope(), "template-model:" + version.id());
  }

  private void cloneWarnings(UUID sourceVersionId, UUID destinationVersionId) {
    List<DocumentConversionWarning> source =
        warnings.findAllByTemplateVersionIdOrderByPositionAsc(sourceVersionId);
    for (DocumentConversionWarning warning : source) {
      warnings.save(
          new DocumentConversionWarning(
              UUID.randomUUID(),
              destinationVersionId,
              warning.severity(),
              warning.code(),
              warning.message(),
              warning.sourceNodeId(),
              warning.pageNumber(),
              warning.fallback(),
              warning.blocking(),
              warning.position()));
    }
  }

  private int nextVersionNumber(UUID templateId) {
    return versions.findAllByTemplateIdOrderByVersionNumberDesc(templateId).stream()
            .findFirst()
            .map(DocumentTemplateVersion::versionNumber)
            .orElse(0)
        + 1;
  }

  private DocumentTemplateOperation findOperation(
      UUID userId, DocumentTemplateOperationType type, String key) {
    return operations.findByActorIdAndOperationTypeAndIdempotencyKey(userId, type, key).orElse(null);
  }

  private DocumentTemplateVersionResponse replayVersion(
      DocumentTemplateOperation operation, UUID templateId, String requestHash) {
    if (!operation.templateId().equals(templateId)
        || !operation.requestHash().equals(requestHash)) throw idempotencyConflict();
    return versionResponse(versions.findById(operation.resultVersionId()).orElseThrow(this::notFound));
  }

  private void saveOperation(
      UUID templateId,
      UUID versionId,
      DocumentTemplateOperationType type,
      String key,
      String requestHash,
      UUID actorId,
      Instant now) {
    operations.save(
        new DocumentTemplateOperation(
            templateId, versionId, type, key, requestHash, actorId, now));
  }

  private void validateKey(String key) {
    if (key == null || key.isBlank() || key.length() > 128) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          ApiErrorCode.VALIDATION_FAILED,
          ApiErrorCategory.VALIDATION,
          "Idempotency-Key 长度必须为 1 到 128 个字符");
    }
  }

  private ApiException conflict(String message) {
    return new ApiException(
        HttpStatus.CONFLICT,
        ApiErrorCode.VERSION_CONFLICT,
        ApiErrorCategory.CONFLICT,
        message);
  }

  private ApiException idempotencyConflict() {
    return new ApiException(
        HttpStatus.CONFLICT,
        ApiErrorCode.IDEMPOTENCY_CONFLICT,
        ApiErrorCategory.CONFLICT,
        "幂等键已用于不同的模板操作");
  }

  private ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        "模板、模板版本或资源不存在");
  }

  public record BinaryResource(byte[] bytes, String contentType, String filename) {}
  private record CreateFingerprint(UUID sourceVersionId, String name) {}
  private record VersionFingerprint(
      UUID templateId, UUID baseVersionId, JsonNode documentModel, String changeSummary) {}
  private record PublishFingerprint(UUID templateId, UUID versionId, String note) {}
  private record RollbackFingerprint(
      UUID templateId, UUID targetVersionId, String changeSummary) {}
}
