package com.docmind.api.source.infrastructure;

import com.docmind.api.source.domain.SourceUploadSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SourceUploadSessionRepository
    extends JpaRepository<SourceUploadSession, UUID> {

  Optional<SourceUploadSession> findByWorkspaceIdAndCreatedByAndCreationIdempotencyKey(
      UUID workspaceId, UUID createdBy, String creationIdempotencyKey);

  Optional<SourceUploadSession> findBySourceVersionId(UUID sourceVersionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<SourceUploadSession>
      findTop100ByStagingCleanedAtIsNullAndExpiresAtBeforeOrderByExpiresAtAsc(Instant expiresAt);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select upload from SourceUploadSession upload where upload.sourceVersionId = :versionId")
  Optional<SourceUploadSession> findLockedBySourceVersionId(
      @Param("versionId") UUID sourceVersionId);
}
