package com.docmind.api.identity.api;

import com.docmind.api.identity.domain.WorkspaceMember;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WorkspaceSummaryResponse(
    UUID id, String name, String slug, WorkspaceRole role, Instant createdAt) {

  public static WorkspaceSummaryResponse from(WorkspaceMember member) {
    return new WorkspaceSummaryResponse(
        member.workspace().id(),
        member.workspace().name(),
        member.workspace().slug(),
        member.role(),
        member.workspace().createdAt());
  }
}
