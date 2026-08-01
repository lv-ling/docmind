package com.docmind.api.extraction.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.docmind.api.extraction.ai.AiServiceContracts.SchemaFieldDefinition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiServiceContractSerializationTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
          .serializationInclusion(JsonInclude.Include.NON_NULL)
          .build();

  @Test
  void serializesSchemaDefaultUsingThePythonContractPropertyName() {
    JsonNode literal =
        objectMapper.createObjectNode().put("kind", "literal").put("value", "");
    SchemaFieldDefinition field =
        new SchemaFieldDefinition(
            UUID.randomUUID(),
            "reference",
            "$.reference",
            "Reference",
            "string",
            null,
            true,
            false,
            literal,
            "none",
            objectMapper.createObjectNode(),
            objectMapper.createArrayNode(),
            null,
            0);

    JsonNode serialized = objectMapper.valueToTree(field);

    assertThat(serialized.has("default")).isTrue();
    assertThat(serialized.has("default_value")).isFalse();
    assertThat(serialized.path("default").path("value").textValue()).isEmpty();
    assertThat(serialized.has("array_item_type")).isTrue();
    assertThat(serialized.get("array_item_type").isNull()).isTrue();
    assertThat(serialized.has("extraction_hint")).isTrue();
    assertThat(serialized.get("extraction_hint").isNull()).isTrue();
  }
}
