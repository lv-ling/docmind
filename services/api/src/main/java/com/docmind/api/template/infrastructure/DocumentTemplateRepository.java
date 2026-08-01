package com.docmind.api.template.infrastructure;

import com.docmind.api.template.domain.DocumentTemplate;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID> {
  List<DocumentTemplate> findAllByWorkspaceIdOrderByUpdatedAtDesc(UUID workspaceId);

  Optional<DocumentTemplate> findBySourceVersionIdAndCreatedByAndCreationIdempotencyKey(
      UUID sourceVersionId, UUID createdBy, String idempotencyKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select template from DocumentTemplate template where template.id = :id")
  Optional<DocumentTemplate> findLockedById(@Param("id") UUID id);
}
