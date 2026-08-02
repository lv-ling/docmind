package com.docmind.api.sensitive.infrastructure;

import com.docmind.api.sensitive.domain.SensitiveRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveRuleRepository extends JpaRepository<SensitiveRule, UUID> {

  List<SensitiveRule> findAllByTemplateVersionIdOrderByPositionAsc(UUID templateVersionId);
}
