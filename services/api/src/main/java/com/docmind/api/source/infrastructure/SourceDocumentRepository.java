package com.docmind.api.source.infrastructure;

import com.docmind.api.source.domain.SourceDocument;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, UUID> {

  Optional<SourceDocument> findByIdAndDeletedAtIsNull(UUID id);

  Page<SourceDocument> findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID workspaceId, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select source from SourceDocument source where source.id = :id and source.deletedAt is null")
  Optional<SourceDocument> findLockedById(@Param("id") UUID id);
}
