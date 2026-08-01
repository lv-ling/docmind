package com.docmind.api.schema.infrastructure;

import com.docmind.api.schema.domain.ExtractionSchemaVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionSchemaVersionRepository
    extends JpaRepository<ExtractionSchemaVersion, UUID> {

  Optional<ExtractionSchemaVersion> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

  Optional<ExtractionSchemaVersion>
      findBySchemaIdAndCreatedByAndCreationIdempotencyKey(
          UUID schemaId, UUID createdBy, String creationIdempotencyKey);

  List<ExtractionSchemaVersion> findAllBySchemaIdOrderByVersionNumberDesc(UUID schemaId);
}
