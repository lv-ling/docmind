package com.docmind.api.schema.application;

import static com.docmind.api.shared.web.RequestHashSupport.sha256;

import com.docmind.api.audit.application.AuditRecorder;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.schema.api.CreateSchemaRequest;
import com.docmind.api.schema.api.CreateSchemaTemplateRequest;
import com.docmind.api.schema.api.CreateSchemaVersionRequest;
import com.docmind.api.schema.api.ExtractionSchemaDetailResponse;
import com.docmind.api.schema.api.ExtractionSchemaResponse;
import com.docmind.api.schema.api.SchemaFieldResponse;
import com.docmind.api.schema.api.SchemaTemplateResponse;
import com.docmind.api.schema.api.SchemaVersionResponse;
import com.docmind.api.schema.domain.ExtractionSchema;
import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.docmind.api.schema.domain.ExtractionSchemaVersion;
import com.docmind.api.schema.domain.SchemaTemplate;
import com.docmind.api.schema.domain.SchemaVersionStatus;
import com.docmind.api.schema.infrastructure.ExtractionSchemaFieldRepository;
import com.docmind.api.schema.infrastructure.ExtractionSchemaRepository;
import com.docmind.api.schema.infrastructure.ExtractionSchemaVersionRepository;
import com.docmind.api.schema.infrastructure.SchemaTemplateRepository;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchemaService {

  private final ExtractionSchemaRepository schemas;
  private final ExtractionSchemaVersionRepository versions;
  private final ExtractionSchemaFieldRepository fields;
  private final SchemaTemplateRepository templates;
  private final SchemaDefinitionValidator validator;
  private final SchemaJsonSchemaFactory jsonSchemaFactory;
  private final WorkspaceAccessService access;
  private final AuditRecorder audit;
  private final ObjectMapper objectMapper;

  public SchemaService(
      ExtractionSchemaRepository schemas,
      ExtractionSchemaVersionRepository versions,
      ExtractionSchemaFieldRepository fields,
      SchemaTemplateRepository templates,
      SchemaDefinitionValidator validator,
      SchemaJsonSchemaFactory jsonSchemaFactory,
      WorkspaceAccessService access,
      AuditRecorder audit,
      ObjectMapper objectMapper) {
    this.schemas = schemas;
    this.versions = versions;
    this.fields = fields;
    this.templates = templates;
    this.validator = validator;
    this.jsonSchemaFactory = jsonSchemaFactory;
    this.access = access;
    this.audit = audit;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<ExtractionSchemaResponse> list(UUID userId, UUID workspaceId) {
    access.require(userId, workspaceId, WorkspacePermission.VIEW_WORKSPACE);
    return schemas.findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(workspaceId).stream()
        .map(ExtractionSchemaResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public ExtractionSchemaDetailResponse get(UUID userId, UUID schemaId) {
    ExtractionSchema schema = requireSchema(schemaId);
    access.require(userId, schema.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    return detail(schema);
  }

  @Transactional
  public CreateResult<ExtractionSchemaDetailResponse> create(
      UUID userId,
      UUID workspaceId,
      CreateSchemaRequest request,
      String idempotencyKey,
      UUID requestId) {
    access.require(userId, workspaceId, WorkspacePermission.EDIT_CONTENT);
    validateIdempotencyKey(idempotencyKey);
    List<ValidatedSchemaField> validatedFields = validator.validate(request.fields());
    String requestHash =
        sha256(
            objectMapper,
            Map.of(
                "name", request.name().strip(),
                "description", request.description(),
                "fields", request.fields()));

    ExtractionSchema existing =
        schemas
            .findByWorkspaceIdAndCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(
                workspaceId, userId, idempotencyKey)
            .orElse(null);
    if (existing != null) {
      requireSameHash(existing.creationRequestHash(), requestHash);
      return new CreateResult<>(detail(existing), true);
    }

    Instant now = Instant.now();
    ExtractionSchema schema =
        schemas.saveAndFlush(
            new ExtractionSchema(
                workspaceId,
                request.name().strip(),
                request.description(),
                userId,
                idempotencyKey,
                requestHash,
                now));
    ExtractionSchemaVersion version =
        versions.saveAndFlush(
            new ExtractionSchemaVersion(
                schema.id(),
                workspaceId,
                1,
                jsonSchemaFactory.create(validatedFields),
                "初始版本",
                userId,
                null,
                requestHash,
                now));
    fields.saveAll(toEntities(version.id(), validatedFields));
    schema.publish(version.id(), userId, now);
    schemas.save(schema);
    audit.record(
        workspaceId,
        userId,
        "schema.created",
        "extraction_schema",
        schema.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("version_number", 1, "field_count", validatedFields.size()));
    return new CreateResult<>(detail(schema), false);
  }

  @Transactional
  public CreateResult<SchemaVersionResponse> createVersion(
      UUID userId,
      UUID schemaId,
      CreateSchemaVersionRequest request,
      String idempotencyKey,
      UUID requestId) {
    ExtractionSchema visible = requireSchema(schemaId);
    access.require(userId, visible.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    validateIdempotencyKey(idempotencyKey);
    List<ValidatedSchemaField> validatedFields = validator.validate(request.fields());
    String requestHash =
        sha256(
            objectMapper,
            Map.of("fields", request.fields(), "change_summary", request.changeSummary().strip()));

    ExtractionSchema schema = schemas.findLockedById(schemaId).orElseThrow(this::notFound);
    ExtractionSchemaVersion replay =
        versions
            .findBySchemaIdAndCreatedByAndCreationIdempotencyKey(
                schemaId, userId, idempotencyKey)
            .orElse(null);
    if (replay != null) {
      requireSameHash(replay.creationRequestHash(), requestHash);
      return new CreateResult<>(versionResponse(replay), true);
    }

    ExtractionSchemaVersion current =
        versions.findById(schema.currentVersionId()).orElseThrow(this::notFound);
    current.supersede();
    versions.save(current);
    Instant now = Instant.now();
    ExtractionSchemaVersion version =
        versions.saveAndFlush(
            new ExtractionSchemaVersion(
                schema.id(),
                schema.workspaceId(),
                current.versionNumber() + 1,
                jsonSchemaFactory.create(validatedFields),
                request.changeSummary().strip(),
                userId,
                idempotencyKey,
                requestHash,
                now));
    fields.saveAll(toEntities(version.id(), validatedFields));
    schema.publish(version.id(), userId, now);
    schemas.save(schema);
    audit.record(
        schema.workspaceId(),
        userId,
        "schema.version_created",
        "extraction_schema_version",
        version.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("schema_id", schema.id().toString(), "version_number", version.versionNumber(), "field_count", validatedFields.size()));
    return new CreateResult<>(versionResponse(version), false);
  }

  @Transactional(readOnly = true)
  public List<SchemaTemplateResponse> listTemplates(UUID userId, UUID workspaceId) {
    access.require(userId, workspaceId, WorkspacePermission.VIEW_WORKSPACE);
    return templates.findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(workspaceId).stream()
        .map(SchemaTemplateResponse::from)
        .toList();
  }

  @Transactional
  public CreateResult<SchemaTemplateResponse> createTemplate(
      UUID userId,
      UUID workspaceId,
      CreateSchemaTemplateRequest request,
      String idempotencyKey,
      UUID requestId) {
    access.require(userId, workspaceId, WorkspacePermission.EDIT_CONTENT);
    validateIdempotencyKey(idempotencyKey);
    String requestHash =
        sha256(
            objectMapper,
            Map.of(
                "name", request.name().strip(),
                "description", request.description(),
                "schema_version_id", request.schemaVersionId()));
    SchemaTemplate existing =
        templates
            .findByWorkspaceIdAndCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(
                workspaceId, userId, idempotencyKey)
            .orElse(null);
    if (existing != null) {
      requireSameHash(existing.creationRequestHash(), requestHash);
      return new CreateResult<>(SchemaTemplateResponse.from(existing), true);
    }
    ExtractionSchemaVersion version =
        versions
            .findByIdAndWorkspaceId(request.schemaVersionId(), workspaceId)
            .filter(candidate -> candidate.status() != SchemaVersionStatus.DRAFT)
            .orElseThrow(this::notFound);
    Instant now = Instant.now();
    SchemaTemplate template =
        templates.saveAndFlush(
            new SchemaTemplate(
                workspaceId,
                request.name().strip(),
                request.description(),
                version.id(),
                userId,
                idempotencyKey,
                requestHash,
                now));
    audit.record(
        workspaceId,
        userId,
        "schema_template.created",
        "schema_template",
        template.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("schema_version_id", version.id().toString()));
    return new CreateResult<>(SchemaTemplateResponse.from(template), false);
  }

  private ExtractionSchemaDetailResponse detail(ExtractionSchema schema) {
    List<SchemaVersionResponse> history =
        versions.findAllBySchemaIdOrderByVersionNumberDesc(schema.id()).stream()
            .map(this::versionResponse)
            .toList();
    SchemaVersionResponse current =
        history.stream()
            .filter(version -> version.id().equals(schema.currentVersionId()))
            .findFirst()
            .orElseThrow(this::notFound);
    return new ExtractionSchemaDetailResponse(ExtractionSchemaResponse.from(schema), current, history);
  }

  private SchemaVersionResponse versionResponse(ExtractionSchemaVersion version) {
    List<SchemaFieldResponse> responseFields =
        fields.findAllBySchemaVersionIdOrderByPositionAsc(version.id()).stream()
            .map(SchemaFieldResponse::from)
            .toList();
    return SchemaVersionResponse.from(version, responseFields);
  }

  private List<ExtractionSchemaField> toEntities(
      UUID versionId, List<ValidatedSchemaField> validatedFields) {
    return validatedFields.stream()
        .map(
            field ->
                new ExtractionSchemaField(
                    versionId,
                    field.input().key(),
                    field.input().jsonPath(),
                    field.input().description(),
                    field.valueType(),
                    field.arrayItemType(),
                    field.input().required(),
                    field.input().nullable(),
                    field.defaultKind(),
                    field.defaultValue(),
                    field.sensitivity(),
                    field.constraints(),
                    field.examples(),
                    field.input().extractionHint(),
                    field.display(),
                    field.metadata(),
                    field.input().position()))
        .toList();
  }

  private ExtractionSchema requireSchema(UUID schemaId) {
    return schemas.findByIdAndDeletedAtIsNull(schemaId).orElseThrow(this::notFound);
  }

  private void validateIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
      throw validationError("Idempotency-Key 长度必须为 1 到 128 个字符");
    }
  }

  private void requireSameHash(String existingHash, String requestHash) {
    if (!existingHash.equals(requestHash)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ApiErrorCode.IDEMPOTENCY_CONFLICT,
          ApiErrorCategory.CONFLICT,
          "幂等键已用于不同的请求");
    }
  }

  private ApiException validationError(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_FAILED,
        ApiErrorCategory.VALIDATION,
        message);
  }

  private ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        "Schema 或版本不存在");
  }

  public record CreateResult<T>(T response, boolean replayed) {}
}
