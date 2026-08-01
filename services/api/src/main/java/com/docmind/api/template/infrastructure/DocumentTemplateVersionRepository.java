package com.docmind.api.template.infrastructure;

import com.docmind.api.template.domain.DocumentTemplateVersion;
import com.docmind.api.template.domain.TemplateVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTemplateVersionRepository
    extends JpaRepository<DocumentTemplateVersion, UUID> {
  List<DocumentTemplateVersion> findAllByTemplateIdOrderByVersionNumberDesc(UUID templateId);

  Optional<DocumentTemplateVersion> findByIdAndTemplateId(UUID id, UUID templateId);

  Optional<DocumentTemplateVersion> findFirstByTemplateIdAndStatusOrderByVersionNumberDesc(
      UUID templateId, TemplateVersionStatus status);
}
