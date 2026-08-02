package com.docmind.api.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ContractArtifactTest {

  private static final Path CONTRACTS = Path.of("contracts");
  private static final Pattern OPERATION_ID =
      Pattern.compile("^\\s+operationId:\\s+(\\S+)$", Pattern.MULTILINE);
  private static final Pattern LOCAL_COMPONENT_REFERENCE =
      Pattern.compile("#/components/(?:schemas|parameters|responses)/([A-Za-z0-9]+)");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void publishesAConsistentOpenApiContract() throws IOException {
    String document = Files.readString(CONTRACTS.resolve("openapi/v1.yaml"));

    assertThat(document).contains("openapi: 3.1.0", "bearerAuth:");

    var operationIds =
        OPERATION_ID.matcher(document).results().map(result -> result.group(1)).toList();
    assertThat(operationIds).hasSizeGreaterThanOrEqualTo(20);
    assertThat(new HashSet<>(operationIds)).hasSameSizeAs(operationIds);

    var references =
        LOCAL_COMPONENT_REFERENCE.matcher(document).results().map(result -> result.group(1)).toList();
    for (String reference : references) {
      assertThat(document).containsPattern("(?m)^ {4}" + Pattern.quote(reference) + ":");
    }
  }

  @Test
  void publishesValidServerOwnedJsonSchemas() throws IOException {
    Path schemaDirectory = CONTRACTS.resolve("json-schema");
    try (Stream<Path> paths = Files.list(schemaDirectory)) {
      var schemaFiles =
          paths.filter(path -> path.getFileName().toString().endsWith(".schema.json")).toList();
      assertThat(schemaFiles).hasSizeGreaterThanOrEqualTo(2);

      for (Path schemaFile : schemaFiles) {
        JsonNode schema = objectMapper.readTree(schemaFile.toFile());
        String fileName = schemaFile.getFileName().toString();
        assertThat(schema.path("$schema").asText())
            .isEqualTo("https://json-schema.org/draft/2020-12/schema");
        assertThat(schema.path("$id").asText())
            .isEqualTo("https://docmind.local/schemas/" + fileName);
        assertThat(schema.path("title").asText()).isNotBlank();
      }
    }
  }
}
