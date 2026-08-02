package com.docmind.api.template.infrastructure;

import com.docmind.api.template.domain.ParsedContent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParsedContentRepository extends JpaRepository<ParsedContent, UUID> {}
