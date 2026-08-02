package com.docmind.api.schema.infrastructure;

import com.docmind.api.schema.domain.ExtractionSchema;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractionSchemaRepository extends JpaRepository<ExtractionSchema, UUID> {

  Optional<ExtractionSchema> findByIdAndDeletedAtIsNull(UUID id);

  Optional<ExtractionSchema>
      findByWorkspaceIdAndCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(
          UUID workspaceId, UUID createdBy, String creationIdempotencyKey);

  List<ExtractionSchema> findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
      UUID workspaceId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select schema from ExtractionSchema schema where schema.id = :id and schema.deletedAt is null")
  Optional<ExtractionSchema> findLockedById(@Param("id") UUID id);
}
