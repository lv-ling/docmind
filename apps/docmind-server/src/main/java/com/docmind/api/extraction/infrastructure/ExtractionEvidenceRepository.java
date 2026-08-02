package com.docmind.api.extraction.infrastructure;

import com.docmind.api.extraction.domain.ExtractionEvidence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionEvidenceRepository extends JpaRepository<ExtractionEvidence, UUID> {

  List<ExtractionEvidence> findAllByFieldResultIdOrderByPositionAsc(UUID fieldResultId);

  List<ExtractionEvidence> findAllByFieldResultIdAndCandidateIdIsNullOrderByPositionAsc(
      UUID fieldResultId);

  List<ExtractionEvidence> findAllByFieldResultIdAndCandidateIdOrderByPositionAsc(
      UUID fieldResultId, UUID candidateId);
}
