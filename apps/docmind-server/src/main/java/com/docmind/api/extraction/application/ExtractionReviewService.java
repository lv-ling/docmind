package com.docmind.api.extraction.application;

import static com.docmind.api.shared.web.RequestHashSupport.sha256;

import com.docmind.api.audit.application.AuditRecorder;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.extraction.api.ApproveExtractionRequest;
import com.docmind.api.extraction.api.ExtractionRunResponse;
import com.docmind.api.extraction.api.ReviewExtractionFieldRequest;
import com.docmind.api.extraction.domain.ExtractionFieldResult;
import com.docmind.api.extraction.domain.ExtractionReviewOperation;
import com.docmind.api.extraction.domain.ExtractionReviewOperationType;
import com.docmind.api.extraction.domain.ExtractionRun;
import com.docmind.api.extraction.domain.ExtractionRunStatus;
import com.docmind.api.extraction.domain.FieldReviewStatus;
import com.docmind.api.extraction.infrastructure.ExtractionFieldResultRepository;
import com.docmind.api.extraction.infrastructure.ExtractionReviewOperationRepository;
import com.docmind.api.extraction.infrastructure.ExtractionRunRepository;
import com.docmind.api.infrastructure.crypto.JsonEnvelopeEncryption;
import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.docmind.api.schema.domain.SchemaValueType;
import com.docmind.api.schema.infrastructure.ExtractionSchemaFieldRepository;
import com.docmind.api.schema.infrastructure.ExtractionSchemaVersionRepository;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ExtractionReviewService {

  private final ExtractionRunRepository runs;
  private final ExtractionFieldResultRepository fields;
  private final ExtractionSchemaFieldRepository schemaFields;
  private final ExtractionSchemaVersionRepository schemaVersions;
  private final ExtractionReviewOperationRepository operations;
  private final WorkspaceAccessService access;
  private final ExtractionResultViewAssembler assembler;
  private final ExtractionResultValidator resultValidator;
  private final JsonEnvelopeEncryption encryption;
  private final ExtractionEventService events;
  private final AuditRecorder audit;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ExtractionReviewService(
      ExtractionRunRepository runs,
      ExtractionFieldResultRepository fields,
      ExtractionSchemaFieldRepository schemaFields,
      ExtractionSchemaVersionRepository schemaVersions,
      ExtractionReviewOperationRepository operations,
      WorkspaceAccessService access,
      ExtractionResultViewAssembler assembler,
      ExtractionResultValidator resultValidator,
      JsonEnvelopeEncryption encryption,
      ExtractionEventService events,
      AuditRecorder audit,
      ObjectMapper objectMapper) {
    this.runs = runs;
    this.fields = fields;
    this.schemaFields = schemaFields;
    this.schemaVersions = schemaVersions;
    this.operations = operations;
    this.access = access;
    this.assembler = assembler;
    this.resultValidator = resultValidator;
    this.encryption = encryption;
    this.events = events;
    this.audit = audit;
    this.objectMapper = objectMapper;
    this.clock = Clock.systemUTC();
  }

  @Transactional(readOnly = true)
  public ExtractionRunResponse get(UUID userId, UUID extractionId) {
    ExtractionRun run = runs.findById(extractionId).orElseThrow(this::notFound);
    WorkspaceMember membership =
        access.require(userId, run.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    return response(run, membership);
  }

  @Transactional(readOnly = true)
  public SseEmitter subscribe(UUID userId, UUID extractionId) {
    ExtractionRun run = runs.findById(extractionId).orElseThrow(this::notFound);
    access.require(userId, run.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    return events.subscribe(run);
  }

  @Transactional
  public ExtractionRunResponse reviewField(
      UUID userId,
      UUID extractionId,
      UUID fieldResultId,
      ReviewExtractionFieldRequest request,
      String idempotencyKey,
      UUID requestId) {
    ExtractionRun run = runs.findLockedById(extractionId).orElseThrow(this::notFound);
    WorkspaceMember membership =
        access.require(userId, run.workspaceId(), WorkspacePermission.REVIEW_CONTENT);
    validateIdempotencyKey(idempotencyKey);
    String requestHash =
        sha256(
            objectMapper,
            new FieldReviewFingerprint(
                extractionId, fieldResultId, request.action(), request.value(), request.reason()));
    ExtractionReviewOperation replay =
        operations
            .findByActorIdAndOperationTypeAndIdempotencyKey(
                userId, ExtractionReviewOperationType.FIELD_REVIEW, idempotencyKey)
            .orElse(null);
    if (replay != null) {
      requireReplayMatches(replay, extractionId, fieldResultId, requestHash);
      return response(run, membership);
    }
    requireReviewable(run);
    ExtractionFieldResult field =
        fields.findByIdAndExtractionRunId(fieldResultId, extractionId).orElseThrow(this::notFound);
    ExtractionSchemaField schemaField =
        schemaFields.findById(field.schemaFieldId()).orElseThrow(this::notFound);
    FieldReviewStatus action = parseAction(request.action());
    JsonNode encryptedValue = null;
    if (action == FieldReviewStatus.MODIFIED) {
      if (request.value() == null) {
        throw validation("修改字段时必须提供 value，显式 null 请传 JSON null");
      }
      validateValue(schemaField, request.value());
      encryptedValue =
          encryption.encrypt(
              request.value(),
              "extraction-field-review:" + extractionId + ":" + fieldResultId);
    } else if (request.value() != null && !request.value().isNull()) {
      throw validation("接受或拒绝字段时 value 必须为 null");
    }
    if (action == FieldReviewStatus.REJECTED
        && (request.reason() == null || request.reason().isBlank())) {
      throw validation("拒绝字段时必须填写原因");
    }
    Instant now = clock.instant();
    field.review(action, encryptedValue, trimToNull(request.reason()), userId, now);
    operations.save(
        new ExtractionReviewOperation(
            run.workspaceId(),
            extractionId,
            fieldResultId,
            ExtractionReviewOperationType.FIELD_REVIEW,
            idempotencyKey,
            requestHash,
            userId,
            now));
    audit.record(
        run.workspaceId(),
        userId,
        "extraction.field_reviewed",
        "extraction_field_result",
        fieldResultId,
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("extraction_id", extractionId.toString(), "action", action.name()));
    events.publishAfterCommit(run);
    return response(run, membership);
  }

  @Transactional
  public ExtractionRunResponse approve(
      UUID userId,
      UUID extractionId,
      ApproveExtractionRequest request,
      String idempotencyKey,
      UUID requestId) {
    ExtractionRun run = runs.findLockedById(extractionId).orElseThrow(this::notFound);
    WorkspaceMember membership =
        access.require(userId, run.workspaceId(), WorkspacePermission.REVIEW_CONTENT);
    validateIdempotencyKey(idempotencyKey);
    String requestHash =
        sha256(objectMapper, new ApprovalFingerprint(extractionId, request.note()));
    ExtractionReviewOperation replay =
        operations
            .findByActorIdAndOperationTypeAndIdempotencyKey(
                userId, ExtractionReviewOperationType.APPROVE, idempotencyKey)
            .orElse(null);
    if (replay != null) {
      requireReplayMatches(replay, extractionId, null, requestHash);
      return response(run, membership);
    }
    requireReviewable(run);
    if (fields.existsByExtractionRunIdAndReviewStatus(extractionId, FieldReviewStatus.PENDING)) {
      throw conflict("仍有字段尚未完成复核");
    }
    List<ExtractionFieldResult> runFields =
        fields.findAllByExtractionRunIdOrderByJsonPathAsc(extractionId);
    if (runFields.isEmpty()) throw conflict("抽取结果中没有可批准的字段");
    ObjectNode finalData = assembler.buildEffectiveData(run, runFields);
    JsonNode jsonSchema =
        schemaVersions.findById(run.schemaVersionId()).orElseThrow(this::notFound).jsonSchema();
    List<String> validationErrors = resultValidator.validateData(jsonSchema, finalData);
    if (!validationErrors.isEmpty()) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ApiErrorCode.SCHEMA_INVALID,
          ApiErrorCategory.VALIDATION,
          "复核后的结果不满足字段 Schema",
          Map.of("validation_errors", validationErrors),
          List.of());
    }
    Instant now = clock.instant();
    run.approve(
        encryption.encrypt(finalData, "extraction-result:" + extractionId),
        userId,
        trimToNull(request.note()),
        now);
    operations.save(
        new ExtractionReviewOperation(
            run.workspaceId(),
            extractionId,
            null,
            ExtractionReviewOperationType.APPROVE,
            idempotencyKey,
            requestHash,
            userId,
            now));
    audit.record(
        run.workspaceId(),
        userId,
        "extraction.approved",
        "extraction_run",
        extractionId,
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("field_count", runFields.size()));
    events.publishAfterCommit(run);
    return response(run, membership);
  }

  private ExtractionRunResponse response(ExtractionRun run, WorkspaceMember membership) {
    return ExtractionRunResponse.from(run, assembler.assemble(run, membership.role()));
  }

  private void requireReviewable(ExtractionRun run) {
    if (run.status() != ExtractionRunStatus.REVIEW_REQUIRED) {
      throw conflict("抽取任务当前状态不允许复核");
    }
  }

  private FieldReviewStatus parseAction(String action) {
    return switch (action) {
      case "accept" -> FieldReviewStatus.ACCEPTED;
      case "modify" -> FieldReviewStatus.MODIFIED;
      case "reject" -> FieldReviewStatus.REJECTED;
      default -> throw validation("action 必须是 accept、modify 或 reject");
    };
  }

  private void validateValue(ExtractionSchemaField field, JsonNode value) {
    if (value.isNull()) {
      if (!field.nullable()) throw validation("该字段不允许 null");
      return;
    }
    SchemaValueType type = field.valueType();
    boolean valid =
        switch (type) {
          case STRING, DATE, DATETIME -> value.isTextual();
          case NUMBER -> value.isNumber();
          case INTEGER -> value.isIntegralNumber();
          case BOOLEAN -> value.isBoolean();
          case OBJECT -> value.isObject();
          case ARRAY -> value.isArray();
        };
    if (!valid) throw validation("修正值与字段数据类型不匹配");
  }

  private void requireReplayMatches(
      ExtractionReviewOperation operation,
      UUID extractionId,
      UUID fieldResultId,
      String requestHash) {
    if (!operation.extractionRunId().equals(extractionId)
        || !java.util.Objects.equals(operation.fieldResultId(), fieldResultId)
        || !operation.requestHash().equals(requestHash)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ApiErrorCode.IDEMPOTENCY_CONFLICT,
          ApiErrorCategory.CONFLICT,
          "幂等键已用于不同的复核请求");
    }
  }

  private void validateIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
      throw validation("Idempotency-Key 长度必须为 1 到 128 个字符");
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private ApiException validation(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_FAILED,
        ApiErrorCategory.VALIDATION,
        message);
  }

  private ApiException conflict(String message) {
    return new ApiException(
        HttpStatus.CONFLICT,
        ApiErrorCode.VERSION_CONFLICT,
        ApiErrorCategory.CONFLICT,
        message);
  }

  private ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        "抽取任务或字段不存在");
  }

  private record FieldReviewFingerprint(
      UUID extractionId,
      UUID fieldResultId,
      String action,
      JsonNode value,
      String reason) {}

  private record ApprovalFingerprint(UUID extractionId, String note) {}
}
