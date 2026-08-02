package com.docmind.api.extraction.infrastructure;

import com.docmind.api.extraction.domain.SensitiveToken;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveTokenRepository extends JpaRepository<SensitiveToken, UUID> {

  List<SensitiveToken> findAllByExtractionRunIdOrderByTokenAsc(UUID extractionRunId);

  void deleteAllByExtractionRunId(UUID extractionRunId);
}
