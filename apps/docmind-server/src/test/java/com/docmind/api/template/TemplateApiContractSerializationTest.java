package com.docmind.api.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.docmind.api.template.api.DocumentTemplateDetailResponse;
import com.docmind.api.template.api.DocumentTemplateResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemplateApiContractSerializationTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .serializationInclusion(JsonInclude.Include.NON_NULL)
          .build();

  @Test
  void preservesNullableFieldsWhileTemplateConversionIsQueued() {
    UUID templateId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-01T00:00:00Z");
    var template =
        new DocumentTemplateResponse(
            templateId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "排队中的模板",
            null,
            "queued",
            null,
            now,
            actorId,
            now,
            actorId);

    var serialized =
        objectMapper.valueToTree(new DocumentTemplateDetailResponse(template, null, List.of()));

    assertThat(serialized.has("current_version")).isTrue();
    assertThat(serialized.get("current_version").isNull()).isTrue();
    assertThat(serialized.path("template").has("current_version_id")).isTrue();
    assertThat(serialized.path("template").get("current_version_id").isNull()).isTrue();
    assertThat(serialized.path("template").has("failure_code")).isTrue();
    assertThat(serialized.path("template").get("failure_code").isNull()).isTrue();
  }
}
