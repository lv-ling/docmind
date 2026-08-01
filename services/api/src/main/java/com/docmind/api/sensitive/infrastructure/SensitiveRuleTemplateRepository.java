package com.docmind.api.sensitive.infrastructure;

import com.docmind.api.sensitive.domain.SensitiveRuleTemplate;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SensitiveRuleTemplateRepository
    extends JpaRepository<SensitiveRuleTemplate, UUID> {

  Optional<SensitiveRuleTemplate> findByIdAndDeletedAtIsNull(UUID id);

  Optional<SensitiveRuleTemplate>
      findByWorkspaceIdAndCreatedByAndCreationIdempotencyKeyAndDeletedAtIsNull(
          UUID workspaceId, UUID createdBy, String creationIdempotencyKey);

  List<SensitiveRuleTemplate> findAllByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
      UUID workspaceId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select template from SensitiveRuleTemplate template where template.id = :id and template.deletedAt is null")
  Optional<SensitiveRuleTemplate> findLockedById(@Param("id") UUID id);
}
