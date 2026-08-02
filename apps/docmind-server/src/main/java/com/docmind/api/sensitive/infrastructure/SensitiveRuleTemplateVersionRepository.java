package com.docmind.api.sensitive.infrastructure;

import com.docmind.api.sensitive.domain.SensitiveRuleTemplateVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveRuleTemplateVersionRepository
    extends JpaRepository<SensitiveRuleTemplateVersion, UUID> {

  Optional<SensitiveRuleTemplateVersion> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

  Optional<SensitiveRuleTemplateVersion>
      findByTemplateIdAndCreatedByAndCreationIdempotencyKey(
          UUID templateId, UUID createdBy, String creationIdempotencyKey);

  List<SensitiveRuleTemplateVersion> findAllByTemplateIdOrderByVersionNumberDesc(UUID templateId);
}
