package com.docmind.api.sensitive.application;

import static com.docmind.api.shared.web.RequestHashSupport.sha256;

import com.docmind.api.audit.application.AuditRecorder;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.identity.application.WorkspaceAccessService;
import com.docmind.api.identity.application.WorkspacePermission;
import com.docmind.api.sensitive.api.CreateSensitiveRuleTemplateRequest;
import com.docmind.api.sensitive.api.CreateSensitiveRuleTemplateVersionRequest;
import com.docmind.api.sensitive.api.SensitiveRuleResponse;
import com.docmind.api.sensitive.api.SensitiveRuleTemplateDetailResponse;
import com.docmind.api.sensitive.api.SensitiveRuleTemplateResponse;
import com.docmind.api.sensitive.api.SensitiveRuleTemplateVersionResponse;
import com.docmind.api.sensitive.domain.SensitiveRule;
import com.docmind.api.sensitive.domain.SensitiveRuleTemplate;
import com.docmind.api.sensitive.domain.SensitiveRuleTemplateVersion;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleRepository;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleTemplateRepository;
import com.docmind.api.sensitive.infrastructure.SensitiveRuleTemplateVersionRepository;
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
public class SensitiveRuleTemplateService {

  private final SensitiveRuleTemplateRepository templates;
  private final SensitiveRuleTemplateVersionRepository versions;
  private final SensitiveRuleRepository rules;
  private final SensitiveRuleDefinitionValidator validator;
  private final WorkspaceAccessService access;
  private final AuditRecorder audit;
  private final ObjectMapper objectMapper;

  public SensitiveRuleTemplateService(
      SensitiveRuleTemplateRepository templates,
      SensitiveRuleTemplateVersionRepository versions,
      SensitiveRuleRepository rules,
      SensitiveRuleDefinitionValidator validator,
      WorkspaceAccessService access,
      AuditRecorder audit,
      ObjectMapper objectMapper) {
    this.templates = templates;
    this.versions = versions;
    this.rules = rules;
    this.validator = validator;
    this.access = access;
    this.audit = audit;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<SensitiveRuleTemplateResponse> list(UUID userId, UUID workspaceId) {
    access.require(userId, workspaceId, WorkspacePermission.VIEW_WORKSPACE);
    return templates.findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(workspaceId).stream()
        .map(SensitiveRuleTemplateResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public SensitiveRuleTemplateDetailResponse get(UUID userId, UUID templateId) {
    SensitiveRuleTemplate template = requireTemplate(templateId);
    access.require(userId, template.workspaceId(), WorkspacePermission.VIEW_WORKSPACE);
    return detail(template);
  }

  @Transactional
  public CreateResult<SensitiveRuleTemplateDetailResponse> create(
      UUID userId,
      UUID workspaceId,
      CreateSensitiveRuleTemplateRequest request,
      String idempotencyKey,
      UUID requestId) {
    access.require(userId, workspaceId, WorkspacePermission.EDIT_CONTENT);
    validateIdempotencyKey(idempotencyKey);
    List<ValidatedSensitiveRule> validatedRules = validator.validate(request.rules());
    String requestHash =
        sha256(
            objectMapper,
            Map.of(
                "name", request.name().strip(),
                "description", request.description(),
                "rules", request.rules()));
    SensitiveRuleTemplate existing =
        templates
            .findByWorkspaceIdAndCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(
                workspaceId, userId, idempotencyKey)
            .orElse(null);
    if (existing != null) {
      requireSameHash(existing.creationRequestHash(), requestHash);
      return new CreateResult<>(detail(existing), true);
    }

    Instant now = Instant.now();
    SensitiveRuleTemplate template =
        templates.saveAndFlush(
            new SensitiveRuleTemplate(
                workspaceId,
                request.name().strip(),
                request.description(),
                userId,
                idempotencyKey,
                requestHash,
                now));
    SensitiveRuleTemplateVersion version =
        versions.saveAndFlush(
            new SensitiveRuleTemplateVersion(
                template.id(),
                workspaceId,
                1,
                "初始版本",
                userId,
                null,
                requestHash,
                now));
    rules.saveAll(toEntities(version.id(), validatedRules));
    template.publish(version.id(), userId, now);
    templates.save(template);
    audit.record(
        workspaceId,
        userId,
        "sensitive_rule_template.created",
        "sensitive_rule_template",
        template.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("version_number", 1, "rule_count", validatedRules.size()));
    return new CreateResult<>(detail(template), false);
  }

  @Transactional
  public CreateResult<SensitiveRuleTemplateVersionResponse> createVersion(
      UUID userId,
      UUID templateId,
      CreateSensitiveRuleTemplateVersionRequest request,
      String idempotencyKey,
      UUID requestId) {
    SensitiveRuleTemplate visible = requireTemplate(templateId);
    access.require(userId, visible.workspaceId(), WorkspacePermission.EDIT_CONTENT);
    validateIdempotencyKey(idempotencyKey);
    List<ValidatedSensitiveRule> validatedRules = validator.validate(request.rules());
    String requestHash =
        sha256(
            objectMapper,
            Map.of("rules", request.rules(), "change_summary", request.changeSummary().strip()));

    SensitiveRuleTemplate template =
        templates.findLockedById(templateId).orElseThrow(this::notFound);
    SensitiveRuleTemplateVersion replay =
        versions
            .findByTemplateIdAndCreatedByAndCreationIdempotencyKey(
                templateId, userId, idempotencyKey)
            .orElse(null);
    if (replay != null) {
      requireSameHash(replay.creationRequestHash(), requestHash);
      return new CreateResult<>(versionResponse(replay), true);
    }

    SensitiveRuleTemplateVersion current =
        versions.findById(template.currentVersionId()).orElseThrow(this::notFound);
    current.supersede();
    versions.save(current);
    Instant now = Instant.now();
    SensitiveRuleTemplateVersion version =
        versions.saveAndFlush(
            new SensitiveRuleTemplateVersion(
                template.id(),
                template.workspaceId(),
                current.versionNumber() + 1,
                request.changeSummary().strip(),
                userId,
                idempotencyKey,
                requestHash,
                now));
    rules.saveAll(toEntities(version.id(), validatedRules));
    template.publish(version.id(), userId, now);
    templates.save(template);
    audit.record(
        template.workspaceId(),
        userId,
        "sensitive_rule_template.version_created",
        "sensitive_rule_template_version",
        version.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of(
            "template_id",
            template.id().toString(),
            "version_number",
            version.versionNumber(),
            "rule_count",
            validatedRules.size()));
    return new CreateResult<>(versionResponse(version), false);
  }

  private SensitiveRuleTemplateDetailResponse detail(SensitiveRuleTemplate template) {
    List<SensitiveRuleTemplateVersionResponse> history =
        versions.findAllByTemplateIdOrderByVersionNumberDesc(template.id()).stream()
            .map(this::versionResponse)
            .toList();
    SensitiveRuleTemplateVersionResponse current =
        history.stream()
            .filter(version -> version.id().equals(template.currentVersionId()))
            .findFirst()
            .orElseThrow(this::notFound);
    return new SensitiveRuleTemplateDetailResponse(
        SensitiveRuleTemplateResponse.from(template), current, history);
  }

  private SensitiveRuleTemplateVersionResponse versionResponse(
      SensitiveRuleTemplateVersion version) {
    List<SensitiveRuleResponse> responseRules =
        rules.findAllByTemplateVersionIdOrderByPositionAsc(version.id()).stream()
            .map(SensitiveRuleResponse::from)
            .toList();
    return SensitiveRuleTemplateVersionResponse.from(version, responseRules);
  }

  private List<SensitiveRule> toEntities(
      UUID versionId, List<ValidatedSensitiveRule> validatedRules) {
    return validatedRules.stream()
        .map(
            rule ->
                new SensitiveRule(
                    versionId,
                    rule.input().key(),
                    rule.input().name().strip(),
                    rule.input().description(),
                    rule.dataType(),
                    rule.recognizerKind(),
                    rule.input().locales(),
                    rule.input().countryCodes(),
                    rule.input().regexPattern(),
                    rule.input().regexDialect(),
                    rule.input().dictionaryTerms(),
                    rule.input().validatorName(),
                    rule.input().confidenceThreshold(),
                    rule.input().priority(),
                    rule.input().enabled(),
                    rule.position()))
        .toList();
  }

  private SensitiveRuleTemplate requireTemplate(UUID templateId) {
    return templates.findByIdAndDeletedAtIsNull(templateId).orElseThrow(this::notFound);
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
          "幂等键已用于不同的请求");
    }
  }

  private ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        "敏感规则模板或版本不存在");
  }

  public record CreateResult<T>(T response, boolean replayed) {}
}
