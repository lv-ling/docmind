package com.docmind.api.identity.infrastructure;

import com.docmind.api.identity.domain.Workspace;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
  Optional<Workspace> findBySlugAndDeletedAtIsNull(String slug);

  Optional<Workspace> findByCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(
      UUID createdBy, String creationIdempotencyKey);
}
