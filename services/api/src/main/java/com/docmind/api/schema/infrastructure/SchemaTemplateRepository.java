package com.docmind.api.schema.infrastructure;

import com.docmind.api.schema.domain.SchemaTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaTemplateRepository extends JpaRepository<SchemaTemplate, UUID> {

  Optional<SchemaTemplate> findByIdAndDeletedAtIsNull(UUID id);

  Optional<SchemaTemplate>
      findByWorkspaceIdAndCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(
          UUID workspaceId, UUID createdBy, String creationIdempotencyKey);

  List<SchemaTemplate> findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
      UUID workspaceId);
}
