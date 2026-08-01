package com.docmind.api.extraction.application;

import com.docmind.api.extraction.api.ExtractionCandidateViewResponse;
import com.docmind.api.extraction.api.ExtractionEvidenceViewResponse;
import com.docmind.api.extraction.api.ExtractionFieldResultViewResponse;
import com.docmind.api.extraction.api.ExtractionModelMetadataResponse;
import com.docmind.api.extraction.api.ExtractionResultViewResponse;
import com.docmind.api.extraction.domain.ExtractionCandidate;
import com.docmind.api.extraction.domain.ExtractionEvidence;
import com.docmind.api.extraction.domain.ExtractionFieldResult;
import com.docmind.api.extraction.domain.FieldReviewStatus;
import com.docmind.api.extraction.infrastructure.ExtractionCandidateRepository;
import com.docmind.api.extraction.infrastructure.ExtractionEvidenceRepository;
import com.docmind.api.extraction.infrastructure.ExtractionFieldResultRepository;
import com.docmind.api.infrastructure.crypto.JsonEnvelopeEncryption;
import com.docmind.api.identity.domain.WorkspaceRole;
import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.docmind.api.schema.domain.FieldSensitivity;
import com.docmind.api.schema.infrastructure.ExtractionSchemaFieldRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExtractionResultViewAssembler {

  private final ExtractionFieldResultRepository fields;
  private final ExtractionCandidateRepository candidates;
  private final ExtractionEvidenceRepository evidence;
  private final ExtractionSchemaFieldRepository schemaFields;
  private final JsonEnvelopeEncryption encryption;
  private final ObjectMapper objectMapper;

  public ExtractionResultViewAssembler(
      ExtractionFieldResultRepository fields,
      ExtractionCandidateRepository candidates,
      ExtractionEvidenceRepository evidence,
      ExtractionSchemaFieldRepository schemaFields,
      JsonEnvelopeEncryption encryption,
      ObjectMapper objectMapper) {
    this.fields = fields;
    this.candidates = candidates;
    this.evidence = evidence;
    this.schemaFields = schemaFields;
    this.encryption = encryption;
    this.objectMapper = objectMapper;
  }

  public ExtractionResultViewResponse assemble(
      com.docmind.api.extraction.domain.ExtractionRun run, WorkspaceRole role) {
    if (run.resultDataEnvelope() == null) return null;
    List<ExtractionFieldResult> results =
        fields.findAllByExtractionRunIdOrderByJsonPathAsc(run.id());
    Map<java.util.UUID, ExtractionSchemaField> schemaById = new HashMap<>();
    schemaFields
        .findAllBySchemaVersionIdOrderByPositionAsc(run.schemaVersionId())
        .forEach(field -> schemaById.put(field.id(), field));

    ObjectNode safeData = objectMapper.createObjectNode();
    List<ExtractionFieldResultViewResponse> safeFields = new ArrayList<>();
    boolean containsMasked = false;
    for (ExtractionFieldResult field : results) {
      ExtractionSchemaField schemaField = schemaById.get(field.schemaFieldId());
      if (schemaField == null) {
        throw new ExtractionExecutionStateException("EXTRACTION_SCHEMA_FIELD_NOT_FOUND");
      }
      boolean visible = canViewValue(schemaField, role);
      JsonNode effectiveValue = visible ? decryptEffectiveValue(run.id(), field) : null;
      String maskedPreview =
          field.reviewStatus() == FieldReviewStatus.MODIFIED
              ? "[受保护的修正值]"
              : field.maskedPreview();
      JsonNode displayValue = displayValue(visible, effectiveValue, maskedPreview);
      setPath(safeData, field.jsonPath(), visible ? effectiveValue : objectMapper.getNodeFactory().textNode(maskedPreview));
      containsMasked |= !visible;
      boolean evidenceVisible = visible && canReviewEvidence(role);
      safeFields.add(
          new ExtractionFieldResultViewResponse(
              field.id(),
              field.jsonPath(),
              displayValue,
              field.reviewStatus() == FieldReviewStatus.MODIFIED
                  ? "manual"
                  : field.valueSource().wireValue(),
              field.missingReason() == null ? null : field.missingReason().wireValue(),
              field.confidence(),
              directEvidence(run.id(), field, evidenceVisible),
              candidateViews(run.id(), field, visible, evidenceVisible),
              field.needsReview(),
              field.reviewStatus().wireValue()));
    }

    return new ExtractionResultViewResponse(
        safeData,
        containsMasked,
        List.copyOf(safeFields),
        new ExtractionModelMetadataResponse(
            run.modelProvider(),
            run.modelName(),
            run.promptVersion(),
            run.inputTokens(),
            run.outputTokens()),
        validationErrors(run));
  }

  public JsonNode decryptEffectiveValue(
      java.util.UUID extractionId, ExtractionFieldResult field) {
    if (field.reviewStatus() == FieldReviewStatus.MODIFIED) {
      return encryption.decrypt(
          field.reviewedValueEnvelope(),
          "extraction-field-review:" + extractionId + ":" + field.id());
    }
    if (field.reviewStatus() == FieldReviewStatus.REJECTED) {
      return objectMapper.nullNode();
    }
    return encryption.decrypt(
        field.valueEnvelope(),
        "extraction-field:" + extractionId + ":" + field.schemaFieldId());
  }

  public ObjectNode buildEffectiveData(
      com.docmind.api.extraction.domain.ExtractionRun run,
      List<ExtractionFieldResult> runFields) {
    ObjectNode data = objectMapper.createObjectNode();
    runFields.forEach(
        field -> setPath(data, field.jsonPath(), decryptEffectiveValue(run.id(), field)));
    return data;
  }

  private List<ExtractionCandidateViewResponse> candidateViews(
      java.util.UUID extractionId,
      ExtractionFieldResult field,
      boolean visible,
      boolean evidenceVisible) {
    return candidates.findAllByFieldResultIdOrderByPositionAsc(field.id()).stream()
        .map(candidate -> candidateView(extractionId, field, candidate, visible, evidenceVisible))
        .toList();
  }

  private ExtractionCandidateViewResponse candidateView(
      java.util.UUID extractionId,
      ExtractionFieldResult field,
      ExtractionCandidate candidate,
      boolean visible,
      boolean evidenceVisible) {
    JsonNode value =
        visible
            ? encryption.decrypt(
                candidate.valueEnvelope(),
                "extraction-candidate:"
                    + extractionId
                    + ":"
                    + field.schemaFieldId()
                    + ":"
                    + candidate.position())
            : null;
    return new ExtractionCandidateViewResponse(
        displayValue(visible, value, candidate.maskedPreview()),
        candidate.confidence(),
        evidence.findAllByFieldResultIdAndCandidateIdOrderByPositionAsc(field.id(), candidate.id())
            .stream()
            .map(item -> evidenceView(extractionId, item, evidenceVisible))
            .toList());
  }

  private List<ExtractionEvidenceViewResponse> directEvidence(
      java.util.UUID extractionId, ExtractionFieldResult field, boolean visible) {
    return evidence.findAllByFieldResultIdAndCandidateIdIsNullOrderByPositionAsc(field.id()).stream()
        .map(item -> evidenceView(extractionId, item, visible))
        .toList();
  }

  private ExtractionEvidenceViewResponse evidenceView(
      java.util.UUID extractionId, ExtractionEvidence item, boolean visible) {
    String text =
        visible
            ? encryption
                .decrypt(
                    item.textEnvelope(),
                    "extraction-evidence:"
                        + extractionId
                        + ":"
                        + item.fieldResultId()
                        + ":"
                        + (item.candidateId() == null ? "direct" : item.candidateId())
                        + ":"
                        + item.position())
                .asText()
            : item.maskedPreview();
    return new ExtractionEvidenceViewResponse(
        item.pageNumber(),
        item.nodeId(),
        text,
        !visible,
        item.startOffset(),
        item.endOffset());
  }

  private JsonNode displayValue(boolean visible, JsonNode value, String maskedPreview) {
    ObjectNode display = objectMapper.createObjectNode();
    display.put("access", visible ? "visible" : "masked");
    if (visible) {
      display.set("value", value == null ? objectMapper.nullNode() : value.deepCopy());
    } else {
      display.set("value", objectMapper.nullNode());
      display.put("masked_preview", maskedPreview);
    }
    return display;
  }

  private boolean canViewValue(ExtractionSchemaField field, WorkspaceRole role) {
    if (field.sensitivity() == FieldSensitivity.NONE
        || "none".equals(field.display().path("mask").asText())) {
      return true;
    }
    JsonNode allowed = field.display().path("view_role_keys");
    if (!allowed.isArray()) return false;
    for (JsonNode item : allowed) {
      if (role.wireValue().equals(item.asText())) return true;
    }
    return false;
  }

  private boolean canReviewEvidence(WorkspaceRole role) {
    return role == WorkspaceRole.OWNER
        || role == WorkspaceRole.ADMIN
        || role == WorkspaceRole.REVIEWER;
  }

  private List<String> validationErrors(
      com.docmind.api.extraction.domain.ExtractionRun run) {
    if (run.validationErrors() == null || !run.validationErrors().isArray()) return List.of();
    List<String> values = new ArrayList<>();
    run.validationErrors().forEach(item -> values.add(item.asText()));
    return List.copyOf(values);
  }

  private void setPath(ObjectNode root, String jsonPath, JsonNode value) {
    if (jsonPath == null || !jsonPath.matches("^\\$(?:\\.[A-Za-z_][A-Za-z0-9_]*)+$")) {
      throw new ExtractionExecutionStateException("EXTRACTION_FIELD_PATH_INVALID");
    }
    String[] parts = jsonPath.substring(2).split("\\.");
    ObjectNode current = root;
    for (int index = 0; index < parts.length - 1; index++) {
      JsonNode child = current.get(parts[index]);
      if (child == null) {
        child = current.putObject(parts[index]);
      }
      if (!(child instanceof ObjectNode objectChild)) {
        throw new ExtractionExecutionStateException("EXTRACTION_FIELD_PATH_CONFLICT");
      }
      current = objectChild;
    }
    current.set(parts[parts.length - 1], value == null ? objectMapper.nullNode() : value.deepCopy());
  }
}
