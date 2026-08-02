package com.docmind.api.source.infrastructure;

import com.docmind.api.source.domain.SourcePreview;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SourcePreviewRepository extends JpaRepository<SourcePreview, UUID> {

  Optional<SourcePreview> findBySourceVersionId(UUID sourceVersionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select preview from SourcePreview preview where preview.id = :previewId")
  Optional<SourcePreview> findLockedById(@Param("previewId") UUID previewId);
}
