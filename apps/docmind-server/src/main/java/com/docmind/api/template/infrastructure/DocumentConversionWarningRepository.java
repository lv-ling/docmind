package com.docmind.api.template.infrastructure;

import com.docmind.api.template.domain.DocumentConversionWarning;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentConversionWarningRepository
    extends JpaRepository<DocumentConversionWarning, UUID> {
  List<DocumentConversionWarning> findAllByTemplateVersionIdOrderByPositionAsc(
      UUID templateVersionId);

  boolean existsByTemplateVersionIdAndBlockingTrue(UUID templateVersionId);
}
