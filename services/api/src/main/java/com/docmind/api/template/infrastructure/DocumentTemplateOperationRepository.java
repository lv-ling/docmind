package com.docmind.api.template.infrastructure;

import com.docmind.api.template.domain.DocumentTemplateOperation;
import com.docmind.api.template.domain.DocumentTemplateOperationType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTemplateOperationRepository
    extends JpaRepository<DocumentTemplateOperation, UUID> {
  Optional<DocumentTemplateOperation> findByActorIdAndOperationTypeAndIdempotencyKey(
      UUID actorId, DocumentTemplateOperationType type, String idempotencyKey);
}
