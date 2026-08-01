package com.docmind.api.identity.application;

import com.docmind.api.identity.domain.MemberStatus;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.identity.infrastructure.WorkspaceMemberRepository;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceAccessService {

  private static final Set<WorkspacePermission> OWNER_PERMISSIONS =
      Set.copyOf(EnumSet.allOf(WorkspacePermission.class));
  private static final Map<WorkspaceRole, Set<WorkspacePermission>> ROLE_PERMISSIONS =
      Map.of(
          WorkspaceRole.OWNER,
          OWNER_PERMISSIONS,
          WorkspaceRole.ADMIN,
          EnumSet.of(
              WorkspacePermission.VIEW_WORKSPACE,
              WorkspacePermission.EDIT_CONTENT,
              WorkspacePermission.REVIEW_CONTENT,
              WorkspacePermission.MANAGE_MEMBERS),
          WorkspaceRole.EDITOR,
          EnumSet.of(WorkspacePermission.VIEW_WORKSPACE, WorkspacePermission.EDIT_CONTENT),
          WorkspaceRole.REVIEWER,
          EnumSet.of(WorkspacePermission.VIEW_WORKSPACE, WorkspacePermission.REVIEW_CONTENT),
          WorkspaceRole.VIEWER,
          EnumSet.of(WorkspacePermission.VIEW_WORKSPACE));

  private final WorkspaceMemberRepository members;

  public WorkspaceAccessService(WorkspaceMemberRepository members) {
    this.members = members;
  }

  @Transactional(readOnly = true)
  public WorkspaceMember require(
      UUID userId, UUID workspaceId, WorkspacePermission permission) {
    WorkspaceMember membership =
        members
            .findByWorkspace_IdAndUser_IdAndDeletedAtIsNull(workspaceId, userId)
            .filter(member -> member.status() == MemberStatus.ACTIVE)
            .orElseThrow(this::workspaceNotFound);

    if (!ROLE_PERMISSIONS.getOrDefault(membership.role(), Set.of()).contains(permission)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          ApiErrorCode.PERMISSION_DENIED,
          ApiErrorCategory.AUTHORIZATION,
          "没有执行此操作的权限");
    }
    return membership;
  }

  private ApiException workspaceNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        ApiErrorCode.RESOURCE_NOT_FOUND,
        ApiErrorCategory.RESOURCE,
        "工作区不存在");
  }
}
