package com.docmind.api.extraction.infrastructure;

import com.docmind.api.extraction.domain.ExtractionReviewOperation;
import com.docmind.api.extraction.domain.ExtractionReviewOperationType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionReviewOperationRepository
    extends JpaRepository<ExtractionReviewOperation, UUID> {

  Optional<ExtractionReviewOperation> findByActorIdAndOperationTypeAndIdempotencyKey(
      UUID actorId, ExtractionReviewOperationType operationType, String idempotencyKey);
}
