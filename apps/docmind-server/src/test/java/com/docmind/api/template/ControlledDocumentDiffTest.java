package com.docmind.api.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.docmind.api.template.application.ControlledDocumentDiff;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ControlledDocumentDiffTest {
  @Test
  void producesAStableBackendDiffWithoutMutatingEitherVersion() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    var before = objectMapper.readTree("{\"title\":\"A\",\"blocks\":[1,2]}");
    var after = objectMapper.readTree("{\"title\":\"B\",\"blocks\":[1,3],\"reviewed\":true}");

    var result = new ControlledDocumentDiff(objectMapper).compare(before, after);

    assertThat(result.path("version").asText()).isEqualTo("1.0");
    assertThat(result.path("truncated").asBoolean()).isFalse();
    assertThat(result.path("changes")).hasSize(3);
    assertThat(result.at("/changes/0/path").asText()).isEqualTo("$.title");
    assertThat(before.path("title").asText()).isEqualTo("A");
    assertThat(after.path("title").asText()).isEqualTo("B");
  }
}
