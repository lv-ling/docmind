package com.docmind.api.source.infrastructure;

import com.docmind.api.source.domain.SourceVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface SourceVersionRepository extends JpaRepository<SourceVersion, UUID> {

  List<SourceVersion> findAllBySourceDocumentIdOrderByVersionNumberDesc(UUID sourceDocumentId);

  Optional<SourceVersion> findTopBySourceDocumentIdOrderByVersionNumberDesc(UUID sourceDocumentId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select version from SourceVersion version where version.id = :versionId")
  Optional<SourceVersion> findLockedById(@Param("versionId") UUID versionId);
}
