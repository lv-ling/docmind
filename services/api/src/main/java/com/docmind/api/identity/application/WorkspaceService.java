package com.docmind.api.identity.application;

import com.docmind.api.audit.application.AuditRecorder;
import com.docmind.api.audit.domain.AuditOutcome;
import com.docmind.api.identity.api.CreateWorkspaceRequest;
import com.docmind.api.identity.api.WorkspaceMemberResponse;
import com.docmind.api.identity.api.WorkspaceSummaryResponse;
import com.docmind.api.identity.domain.MemberStatus;
import com.docmind.api.identity.domain.UserAccount;
import com.docmind.api.identity.domain.Workspace;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.identity.infrastructure.UserAccountRepository;
import com.docmind.api.identity.infrastructure.WorkspaceMemberRepository;
import com.docmind.api.identity.infrastructure.WorkspaceRepository;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

  private static final Pattern SLUG_PATTERN =
      Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");

  private final UserAccountRepository users;
  private final WorkspaceRepository workspaces;
  private final WorkspaceMemberRepository members;
  private final WorkspaceAccessService access;
  private final AuditRecorder audit;

  public WorkspaceService(
      UserAccountRepository users,
      WorkspaceRepository workspaces,
      WorkspaceMemberRepository members,
      WorkspaceAccessService access,
      AuditRecorder audit) {
    this.users = users;
    this.workspaces = workspaces;
    this.members = members;
    this.access = access;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public List<WorkspaceSummaryResponse> listForUser(UUID userId) {
    return members
        .findAllByUser_IdAndStatusAndDeletedAtIsNullOrderByWorkspace_NameAsc(
            userId, MemberStatus.ACTIVE)
        .stream()
        .map(WorkspaceSummaryResponse::from)
        .toList();
  }

  @Transactional
  public CreateWorkspaceResult create(
      UUID userId,
      CreateWorkspaceRequest request,
      String idempotencyKey,
      UUID requestId) {
    String name = request.name().strip();
    String slug = request.slug();
    validate(name, slug, idempotencyKey);
    String requestHash = requestHash(name, slug);

    Workspace existing =
        workspaces
            .findByCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(userId, idempotencyKey)
            .orElse(null);
    if (existing != null) {
      if (!existing.creationRequestHash().equals(requestHash)) {
        throw new ApiException(
            HttpStatus.CONFLICT,
            ApiErrorCode.IDEMPOTENCY_CONFLICT,
            ApiErrorCategory.CONFLICT,
            "幂等键已用于不同的请求");
      }
      WorkspaceMember membership =
          members
              .findByWorkspace_IdAndUser_IdAndDeletedAtIsNull(existing.id(), userId)
              .orElseThrow(() -> new IllegalStateException("workspace owner membership missing"));
      return new CreateWorkspaceResult(WorkspaceSummaryResponse.from(membership), true);
    }

    if (workspaces.findBySlugAndDeletedAtIsNull(slug).isPresent()) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          ApiErrorCode.RESOURCE_CONFLICT,
          ApiErrorCategory.CONFLICT,
          "工作区标识已被使用");
    }

    UserAccount owner =
        users
            .findById(userId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ApiErrorCode.AUTHENTICATION_REQUIRED,
                        ApiErrorCategory.AUTHENTICATION,
                        "需要登录后继续"));
    Workspace workspace =
        workspaces.saveAndFlush(new Workspace(name, slug, userId, idempotencyKey, requestHash));
    WorkspaceMember membership =
        members.save(new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
    audit.record(
        workspace.id(),
        userId,
        "workspace.created",
        "workspace",
        workspace.id(),
        AuditOutcome.SUCCESS,
        requestId,
        Map.of("role", WorkspaceRole.OWNER.wireValue()));
    return new CreateWorkspaceResult(WorkspaceSummaryResponse.from(membership), false);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceMemberResponse> listMembers(UUID userId, UUID workspaceId) {
    access.require(userId, workspaceId, WorkspacePermission.MANAGE_MEMBERS);
    return members
        .findAllByWorkspace_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
            workspaceId, MemberStatus.ACTIVE)
        .stream()
        .map(WorkspaceMemberResponse::from)
        .toList();
  }

  private void validate(String name, String slug, String idempotencyKey) {
    if (name.isBlank() || name.length() > 100) {
      throw validationError("工作区名称长度必须为 1 到 100 个字符");
    }
    if (!SLUG_PATTERN.matcher(slug).matches()) {
      throw validationError("工作区标识格式无效");
    }
    if (idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
      throw validationError("Idempotency-Key 长度必须为 1 到 128 个字符");
    }
  }

  private ApiException validationError(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_FAILED,
        ApiErrorCategory.VALIDATION,
        message);
  }

  private String requestHash(String name, String slug) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(digest.digest((name + "\u0000" + slug).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  public record CreateWorkspaceResult(WorkspaceSummaryResponse workspace, boolean replayed) {}
}
