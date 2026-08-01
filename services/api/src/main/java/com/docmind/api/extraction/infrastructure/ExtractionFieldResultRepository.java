package com.docmind.api.extraction.infrastructure;

import com.docmind.api.extraction.domain.ExtractionFieldResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionFieldResultRepository
    extends JpaRepository<ExtractionFieldResult, UUID> {

  List<ExtractionFieldResult> findAllByExtractionRunIdOrderByJsonPathAsc(UUID extractionRunId);

  java.util.Optional<ExtractionFieldResult> findByIdAndExtractionRunId(
      UUID id, UUID extractionRunId);

  boolean existsByExtractionRunIdAndReviewStatus(
      UUID extractionRunId, com.docmind.api.extraction.domain.FieldReviewStatus reviewStatus);
}
