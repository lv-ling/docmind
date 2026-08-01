package com.docmind.api.identity.api;

import com.docmind.api.identity.domain.MemberStatus;
import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WorkspaceMemberResponse(
    UUID id,
    UUID workspaceId,
    UserSummaryResponse user,
    WorkspaceRole role,
    MemberStatus status,
    Instant createdAt) {

  public static WorkspaceMemberResponse from(WorkspaceMember member) {
    return new WorkspaceMemberResponse(
        member.id(),
        member.workspace().id(),
        UserSummaryResponse.from(member.user()),
        member.role(),
        member.status(),
        member.createdAt());
  }
}
