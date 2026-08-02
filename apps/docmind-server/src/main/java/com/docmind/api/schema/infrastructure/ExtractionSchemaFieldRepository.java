package com.docmind.api.schema.infrastructure;

import com.docmind.api.schema.domain.ExtractionSchemaField;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionSchemaFieldRepository
    extends JpaRepository<ExtractionSchemaField, UUID> {

  List<ExtractionSchemaField> findAllBySchemaVersionIdOrderByPositionAsc(UUID schemaVersionId);
}
