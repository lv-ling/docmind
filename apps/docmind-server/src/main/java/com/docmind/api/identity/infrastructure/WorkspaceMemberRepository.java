package com.docmind.api.identity.infrastructure;

import com.docmind.api.identity.domain.MemberStatus;
import com.docmind.api.identity.domain.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

  @EntityGraph(attributePaths = {"workspace", "user"})
  List<WorkspaceMember> findAllByUser_IdAndStatusAndDeletedAtIsNullOrderByWorkspace_NameAsc(
      UUID userId, MemberStatus status);

  @EntityGraph(attributePaths = {"workspace", "user"})
  Optional<WorkspaceMember> findByWorkspace_IdAndUser_IdAndDeletedAtIsNull(
      UUID workspaceId, UUID userId);

  @EntityGraph(attributePaths = {"workspace", "user"})
  List<WorkspaceMember> findAllByWorkspace_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
      UUID workspaceId, MemberStatus status);
}
