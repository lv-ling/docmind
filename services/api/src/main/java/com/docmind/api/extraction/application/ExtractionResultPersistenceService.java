package com.docmind.api.extraction.application;

import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelCandidateOutput;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelEvidence;
import com.docmind.api.extraction.ai.AiServiceContracts.ModelFieldOutput;
import com.docmind.api.extraction.domain.ExtractionCandidate;
import com.docmind.api.extraction.domain.ExtractionEvidence;
import com.docmind.api.extraction.domain.ExtractionFieldResult;
import com.docmind.api.extraction.domain.ExtractionMissingReason;
import com.docmind.api.extraction.domain.ExtractionRun;
import com.docmind.api.extraction.domain.ExtractionRunStatus;
import com.docmind.api.extraction.domain.ExtractionValueSource;
import com.docmind.api.extraction.infrastructure.ExtractionCandidateRepository;
import com.docmind.api.extraction.infrastructure.ExtractionEvidenceRepository;
import com.docmind.api.extraction.infrastructure.ExtractionFieldResultRepository;
import com.docmind.api.extraction.infrastructure.ExtractionRunRepository;
import com.docmind.api.infrastructure.crypto.JsonEnvelopeEncryption;
import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.docmind.api.schema.domain.FieldSensitivity;
import com.docmind.api.schema.domain.SchemaFieldDefaultKind;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionResultPersistenceService {

  private final ExtractionRunRepository runs;
  private final ExtractionFieldResultRepository fieldResults;
  private final ExtractionCandidateRepository candidates;
  private final ExtractionEvidenceRepository evidence;
  private final JsonEnvelopeEncryption encryption;
  private final SensitiveTokenRestorer restorer;
  private final ExtractionEventService events;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public ExtractionResultPersistenceService(
      ExtractionRunRepository runs,
      ExtractionFieldResultRepository fieldResults,
      ExtractionCandidateRepository candidates,
      ExtractionEvidenceRepository evidence,
      JsonEnvelopeEncryption encryption,
      SensitiveTokenRestorer restorer,
      ExtractionEventService events,
      ObjectMapper objectMapper) {
    this(
        runs,
        fieldResults,
        candidates,
        evidence,
        encryption,
        restorer,
        events,
        objectMapper,
        Clock.systemUTC());
  }

  ExtractionResultPersistenceService(
      ExtractionRunRepository runs,
      ExtractionFieldResultRepository fieldResults,
      ExtractionCandidateRepository candidates,
      ExtractionEvidenceRepository evidence,
      JsonEnvelopeEncryption encryption,
      SensitiveTokenRestorer restorer,
      ExtractionEventService events,
      ObjectMapper objectMapper,
      Clock clock) {
    this.runs = runs;
    this.fieldResults = fieldResults;
    this.candidates = candidates;
    this.evidence = evidence;
    this.encryption = encryption;
    this.restorer = restorer;
    this.events = events;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public void persist(
      ExtractionWorkItem item,
      AiExtractionResponse response,
      SensitiveTokenMapping mapping,
      List<String> validationErrors) {
    ExtractionRun run =
        runs.findLockedById(item.run().id())
            .orElseThrow(() -> new ExtractionExecutionStateException("EXTRACTION_RUN_NOT_FOUND"));
    if (run.status() == ExtractionRunStatus.REVIEW_REQUIRED
        || run.status() == ExtractionRunStatus.APPROVED) {
      return;
    }
    if (run.status() != ExtractionRunStatus.RUNNING) {
      throw new ExtractionExecutionStateException("EXTRACTION_RUN_NOT_RUNNING");
    }
    Map<String, ExtractionSchemaField> schemaByPath = new HashMap<>();
    item.fields().forEach(field -> schemaByPath.put(field.jsonPath(), field));
    Instant now = clock.instant();
    for (ModelFieldOutput output : response.result().fields()) {
      ExtractionSchemaField schemaField = schemaByPath.get(output.path());
      if (schemaField == null) {
        throw new ExtractionResultValidationException("MODEL_FIELD_SET_MISMATCH");
      }
      persistField(item, output, schemaField, mapping, validationErrors, now);
    }

    JsonNode restoredData = restorer.restore(response.result().data(), mapping.replacements());
    ArrayNode safeErrors = objectMapper.createArrayNode();
    validationErrors.forEach(safeErrors::add);
    run.completeForReview(
        encryption.encrypt(restoredData, "extraction-result:" + run.id()),
        mapping.containsSensitiveValues(),
        response.model().provider(),
        response.model().model(),
        response.model().promptVersion(),
        response.model().inputTokens(),
        response.model().outputTokens(),
        safeErrors,
        now);
    events.publishAfterCommit(run);
  }

  private void persistField(
      ExtractionWorkItem item,
      ModelFieldOutput output,
      ExtractionSchemaField schemaField,
      SensitiveTokenMapping mapping,
      List<String> validationErrors,
      Instant now) {
    boolean containsToken =
        restorer.containsKnownToken(output.value(), mapping.replacements());
    boolean formattingAmbiguous =
        containsAnyToken(output.value(), mapping.formattingAmbiguities());
    JsonNode restoredValue = restorer.restore(output.value(), mapping.replacements());
    ExtractionValueSource valueSource = valueSource(schemaField, output.value());
    ExtractionMissingReason missingReason =
        valueSource == ExtractionValueSource.NULL_VALUE
            ? ExtractionMissingReason.NOT_FOUND
            : null;
    boolean needsReview =
        output.needsReview()
            || !output.candidates().isEmpty()
            || !validationErrors.isEmpty()
            || formattingAmbiguous;
    ExtractionFieldResult field =
        fieldResults.saveAndFlush(
            new ExtractionFieldResult(
                item.run().id(),
                schemaField.id(),
                schemaField.jsonPath(),
                encryption.encrypt(
                    restoredValue,
                    "extraction-field:" + item.run().id() + ":" + schemaField.id()),
                preview(restoredValue, schemaField.sensitivity(), containsToken, 512),
                valueSource,
                missingReason,
                output.confidence(),
                needsReview,
                now));
    persistEvidence(item, field.id(), null, output.evidence(), mapping, now);
    for (int position = 0; position < output.candidates().size(); position++) {
      ModelCandidateOutput outputCandidate = output.candidates().get(position);
      boolean candidateContainsToken =
          restorer.containsKnownToken(outputCandidate.value(), mapping.replacements());
      JsonNode restoredCandidate =
          restorer.restore(outputCandidate.value(), mapping.replacements());
      ExtractionCandidate candidate =
          candidates.saveAndFlush(
              new ExtractionCandidate(
                  field.id(),
                  position,
                  encryption.encrypt(
                      restoredCandidate,
                      "extraction-candidate:"
                          + item.run().id()
                          + ":"
                          + schemaField.id()
                          + ":"
                          + position),
                  preview(
                      restoredCandidate,
                      schemaField.sensitivity(),
                      candidateContainsToken,
                      512),
                  outputCandidate.confidence(),
                  now));
      persistEvidence(
          item,
          field.id(),
          candidate.id(),
          outputCandidate.evidence(),
          mapping,
          now);
    }
  }

  private void persistEvidence(
      ExtractionWorkItem item,
      UUID fieldId,
      UUID candidateId,
      List<ModelEvidence> modelEvidence,
      SensitiveTokenMapping mapping,
      Instant now) {
    for (int position = 0; position < modelEvidence.size(); position++) {
      ModelEvidence output = modelEvidence.get(position);
      String restoredText = restorer.restoreText(output.tokenizedText(), mapping.replacements());
      JsonNode restoredNode = objectMapper.getNodeFactory().textNode(restoredText);
      evidence.save(
          new ExtractionEvidence(
              fieldId,
              candidateId,
              item.source().id(),
              position,
              output.pageNumber(),
              output.nodeId(),
              encryption.encrypt(
                  restoredNode,
                  "extraction-evidence:"
                      + item.run().id()
                      + ":"
                      + fieldId
                      + ":"
                      + (candidateId == null ? "direct" : candidateId)
                      + ":"
                      + position),
              truncate(output.tokenizedText(), 1000),
              null,
              null,
              now));
    }
    evidence.flush();
  }

  private ExtractionValueSource valueSource(
      ExtractionSchemaField schemaField, JsonNode tokenizedValue) {
    if (schemaField.defaultKind() == SchemaFieldDefaultKind.LITERAL
        && tokenizedValue.equals(schemaField.defaultValue())) {
      return ExtractionValueSource.DEFAULT;
    }
    return tokenizedValue.isNull()
        ? ExtractionValueSource.NULL_VALUE
        : ExtractionValueSource.EXTRACTED;
  }

  private boolean containsAnyToken(JsonNode value, Set<String> tokens) {
    if (value == null || tokens.isEmpty()) {
      return false;
    }
    if (value.isTextual()) {
      return tokens.stream().anyMatch(value.textValue()::contains);
    }
    if (value.isContainerNode()) {
      for (JsonNode child : value) {
        if (containsAnyToken(child, tokens)) {
          return true;
        }
      }
    }
    return false;
  }

  private String preview(
      JsonNode restoredValue,
      FieldSensitivity sensitivity,
      boolean containsToken,
      int maxLength) {
    if (sensitivity != FieldSensitivity.NONE || containsToken) {
      return "[敏感内容]";
    }
    try {
      return truncate(objectMapper.writeValueAsString(restoredValue), maxLength);
    } catch (JsonProcessingException exception) {
      return "[不可预览]";
    }
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.isEmpty()) {
      return "[空]";
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
  }
}
