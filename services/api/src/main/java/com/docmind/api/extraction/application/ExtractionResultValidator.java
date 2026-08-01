package com.docmind.api.extraction.application;

import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelEvidence;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelFieldOutput;
import com.docmind.api.extraction.ai.AiServiceContracts.TokenizedDocumentNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ExtractionResultValidator {

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("\\[\\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}]]");
  private static final String TOKEN_PREFIX = "[[SENSITIVE:";

  private final ObjectMapper objectMapper;
  private final SensitiveTokenRestorer restorer;
  private final SchemaRegistry schemaRegistry =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

  public ExtractionResultValidator(ObjectMapper objectMapper, SensitiveTokenRestorer restorer) {
    this.objectMapper = objectMapper;
    this.restorer = restorer;
  }

  public List<String> validate(AiExtractionRequest request, AiExtractionResponse response) {
    return validate(request, response, Map.of());
  }

  public List<String> validate(
      AiExtractionRequest request,
      AiExtractionResponse response,
      Map<String, String> sensitiveReplacements) {
    requireResponseIdentity(request, response);
    requireFieldSetAndData(request, response);
    requireEvidenceAligned(request, response);
    requireKnownTokens(request, response);
    requireNoKnownPlaintext(response, sensitiveReplacements);
    requireProviderValidationErrorsSafe(response.validationErrors());
    JsonNode dataForSchema =
        sensitiveReplacements.isEmpty()
            ? response.result().data()
            : restorer.restore(response.result().data(), sensitiveReplacements);
    List<String> independentErrors = validateData(request.jsonSchema(), dataForSchema);
    LinkedHashSet<String> combined = new LinkedHashSet<>(independentErrors);
    return List.copyOf(combined);
  }

  private void requireProviderValidationErrorsSafe(List<String> errors) {
    if (errors == null || errors.size() > 200 || errors.stream().anyMatch(error -> !isSafeValidationError(error))) {
      throw new ExtractionResultValidationException("AI_VALIDATION_ERRORS_INVALID");
    }
  }

  private void requireNoKnownPlaintext(
      AiExtractionResponse response, Map<String, String> sensitiveReplacements) {
    if (sensitiveReplacements.isEmpty()) {
      return;
    }
    String serialized;
    try {
      serialized = objectMapper.writeValueAsString(response.result());
    } catch (JsonProcessingException exception) {
      throw new ExtractionResultValidationException("MODEL_OUTPUT_SERIALIZATION_FAILED", exception);
    }
    for (String plaintext : sensitiveReplacements.values()) {
      if (plaintext != null && !plaintext.isBlank() && serialized.contains(plaintext)) {
        throw new ExtractionResultValidationException("MODEL_PII_LEAK_DETECTED");
      }
    }
  }

  private void requireResponseIdentity(
      AiExtractionRequest request, AiExtractionResponse response) {
    if (response == null
        || response.result() == null
        || response.model() == null
        || !request.requestId().equals(response.requestId())
        || !request.jobId().equals(response.jobId())
        || !request.extractionRunId().equals(response.extractionRunId())
        || response.model().provider() == null
        || response.model().provider().isBlank()
        || response.model().model() == null
        || response.model().model().isBlank()
        || response.model().promptVersion() == null
        || response.model().promptVersion().isBlank()) {
      throw new ExtractionResultValidationException("AI_RESPONSE_IDENTITY_INVALID");
    }
  }

  private void requireFieldSetAndData(
      AiExtractionRequest request, AiExtractionResponse response) {
    if (response.result().data() == null || !response.result().data().isObject()) {
      throw new ExtractionResultValidationException("MODEL_DATA_INVALID");
    }
    Set<String> expected = new HashSet<>();
    request.fields().forEach(field -> expected.add(field.jsonPath()));
    Set<String> actual = new HashSet<>();
    for (ModelFieldOutput field : safeFields(response)) {
      if (field == null
          || field.path() == null
          || !actual.add(field.path())
          || field.value() == null
          || !confidenceValid(field.confidence())) {
        throw new ExtractionResultValidationException("MODEL_FIELD_INVALID");
      }
      JsonNode dataValue = resolve(response.result().data(), field.path());
      if (!field.value().equals(dataValue)) {
        throw new ExtractionResultValidationException("MODEL_DATA_FIELD_MISMATCH");
      }
      if (field.candidates() == null || field.evidence() == null) {
        throw new ExtractionResultValidationException("MODEL_FIELD_INVALID");
      }
      if (!field.candidates().isEmpty() && !field.needsReview()) {
        throw new ExtractionResultValidationException("MODEL_CANDIDATES_REVIEW_MISMATCH");
      }
      field.candidates().forEach(
          candidate -> {
            if (candidate == null
                || candidate.value() == null
                || !confidenceValid(candidate.confidence())
                || candidate.confidence() == null
                || candidate.evidence() == null
                || candidate.evidence().isEmpty()) {
              throw new ExtractionResultValidationException("MODEL_CANDIDATE_INVALID");
            }
          });
    }
    if (!actual.equals(expected) || actual.size() != safeFields(response).size()) {
      throw new ExtractionResultValidationException("MODEL_FIELD_SET_MISMATCH");
    }
  }

  private void requireEvidenceAligned(
      AiExtractionRequest request, AiExtractionResponse response) {
    Map<String, TokenizedDocumentNode> nodes = new HashMap<>();
    request.document().nodes().forEach(node -> nodes.put(node.nodeId(), node));
    for (ModelFieldOutput field : safeFields(response)) {
      field.evidence().forEach(item -> requireEvidenceItem(nodes, item));
      field.candidates().forEach(
          candidate ->
              candidate.evidence().forEach(item -> requireEvidenceItem(nodes, item)));
    }
  }

  private void requireEvidenceItem(
      Map<String, TokenizedDocumentNode> nodes, ModelEvidence evidence) {
    TokenizedDocumentNode node = evidence == null ? null : nodes.get(evidence.nodeId());
    if (node == null
        || evidence.tokenizedText() == null
        || !node.tokenizedText().contains(evidence.tokenizedText())
        || !java.util.Objects.equals(node.pageNumber(), evidence.pageNumber())) {
      throw new ExtractionResultValidationException("MODEL_EVIDENCE_MISMATCH");
    }
  }

  private void requireKnownTokens(
      AiExtractionRequest request, AiExtractionResponse response) {
    Set<String> known = new HashSet<>();
    for (TokenizedDocumentNode node : request.document().nodes()) {
      Matcher matcher = TOKEN_PATTERN.matcher(node.tokenizedText());
      while (matcher.find()) {
        known.add(matcher.group());
      }
    }
    String serialized;
    try {
      serialized = objectMapper.writeValueAsString(response.result());
    } catch (JsonProcessingException exception) {
      throw new ExtractionResultValidationException("MODEL_OUTPUT_SERIALIZATION_FAILED", exception);
    }
    Matcher matcher = TOKEN_PATTERN.matcher(serialized);
    StringBuffer withoutValidTokens = new StringBuffer();
    while (matcher.find()) {
      if (!known.contains(matcher.group())) {
        throw new ExtractionResultValidationException("MODEL_UNKNOWN_SENSITIVE_TOKEN");
      }
      matcher.appendReplacement(withoutValidTokens, "[TOKEN]");
    }
    matcher.appendTail(withoutValidTokens);
    if (withoutValidTokens.indexOf(TOKEN_PREFIX) >= 0) {
      throw new ExtractionResultValidationException("MODEL_MALFORMED_SENSITIVE_TOKEN");
    }
  }

  public List<String> validateData(JsonNode schemaNode, JsonNode data) {
    try {
      Schema schema = schemaRegistry.getSchema(schemaNode);
      List<Error> errors = new ArrayList<>(schema.validate(data));
      errors.sort(
          java.util.Comparator.comparing(error -> error.getInstanceLocation().toString()));
      return errors.stream()
          .limit(200)
          .map(
              error ->
                  safePath(error.getInstanceLocation().toString())
                      + ": "
                      + safeKeyword(error.getKeyword())
                      + " validation failed")
          .toList();
    } catch (RuntimeException exception) {
      throw new ExtractionResultValidationException("REQUEST_JSON_SCHEMA_INVALID", exception);
    }
  }

  private JsonNode resolve(JsonNode data, String path) {
    if (path == null || !path.matches("^\\$(?:\\.[A-Za-z_][A-Za-z0-9_]*)+$")) {
      throw new ExtractionResultValidationException("MODEL_DATA_PATH_INVALID");
    }
    JsonNode current = data;
    for (String part : path.substring(2).split("\\.")) {
      if (!current.isObject() || !current.has(part)) {
        throw new ExtractionResultValidationException("MODEL_DATA_PATH_MISSING");
      }
      current = current.get(part);
    }
    return current;
  }

  private List<ModelFieldOutput> safeFields(AiExtractionResponse response) {
    if (response.result().fields() == null) {
      throw new ExtractionResultValidationException("MODEL_FIELDS_MISSING");
    }
    return response.result().fields();
  }

  private boolean confidenceValid(java.math.BigDecimal confidence) {
    return confidence == null
        || (confidence.compareTo(java.math.BigDecimal.ZERO) >= 0
            && confidence.compareTo(java.math.BigDecimal.ONE) <= 0);
  }

  private boolean isSafeValidationError(String value) {
    return value != null
        && value.length() <= 600
        && value.matches("^\\$[A-Za-z0-9_.\\[\\]-]*: [A-Za-z0-9_-]+ validation failed$");
  }

  private String safePath(String raw) {
    String value = raw == null || raw.isBlank() ? "$" : raw;
    value = value.replace('/', '.');
    return value.matches("^[A-Za-z0-9_$.[\\]-]{1,500}$") ? value : "$";
  }

  private String safeKeyword(String raw) {
    return raw != null && raw.matches("^[A-Za-z][A-Za-z0-9_-]{0,99}$") ? raw : "schema";
  }
}
