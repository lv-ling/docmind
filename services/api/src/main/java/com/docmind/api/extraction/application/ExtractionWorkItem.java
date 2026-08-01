package com.docmind.api.extraction.application;

import com.docmind.api.extraction.domain.ExtractionRun;
import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.docmind.api.schema.domain.ExtractionSchemaVersion;
import com.docmind.api.sensitive.domain.SensitiveRule;
import com.docmind.api.source.domain.SourceVersion;
import java.util.List;

public record ExtractionWorkItem(
    ExtractionRun run,
    SourceVersion source,
    ExtractionSchemaVersion schemaVersion,
    List<ExtractionSchemaField> fields,
    List<SensitiveRule> sensitiveRules) {

  public ExtractionWorkItem {
    fields = List.copyOf(fields);
    sensitiveRules = List.copyOf(sensitiveRules);
  }
}
