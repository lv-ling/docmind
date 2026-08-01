package com.docmind.api.extraction.infrastructure;

import com.docmind.api.extraction.domain.ExtractionRun;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {

  Optional<ExtractionRun> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

  Optional<ExtractionRun> findBySourceVersionIdAndCreatedByAndCreationIdempotencyKey(
      UUID sourceVersionId, UUID createdBy, String creationIdempotencyKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select run from ExtractionRun run where run.id = :id")
  Optional<ExtractionRun> findLockedById(@Param("id") UUID id);
}
