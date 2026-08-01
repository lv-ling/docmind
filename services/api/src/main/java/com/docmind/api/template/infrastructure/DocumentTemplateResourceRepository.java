package com.docmind.api.template.infrastructure;

import com.docmind.api.template.domain.DocumentTemplateResource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTemplateResourceRepository
    extends JpaRepository<DocumentTemplateResource, UUID> {
  List<DocumentTemplateResource> findAllByTemplateVersionIdOrderByOriginalFilenameAsc(
      UUID templateVersionId);
}
