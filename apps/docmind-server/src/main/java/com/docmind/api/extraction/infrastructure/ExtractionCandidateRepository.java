package com.docmind.api.extraction.infrastructure;

import com.docmind.api.extraction.domain.ExtractionCandidate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionCandidateRepository
    extends JpaRepository<ExtractionCandidate, UUID> {

  List<ExtractionCandidate> findAllByFieldResultIdOrderByPositionAsc(UUID fieldResultId);
}
