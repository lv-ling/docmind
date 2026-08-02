package com.docmind.api.extraction.application;

import com.docmind.api.extraction.domain.SensitiveToken;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SensitiveTokenMapping(
    List<SensitiveToken> entities,
    Map<String, String> replacements,
    Set<String> formattingAmbiguities) {

  public SensitiveTokenMapping {
    entities = List.copyOf(entities);
    replacements = Map.copyOf(replacements);
    formattingAmbiguities = Set.copyOf(formattingAmbiguities);
  }

  public boolean containsSensitiveValues() {
    return !entities.isEmpty();
  }
}
