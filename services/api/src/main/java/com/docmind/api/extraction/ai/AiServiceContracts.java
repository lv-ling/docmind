package com.docmind.api.extraction.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AiServiceContracts {

  private AiServiceContracts() {}

  public record ParsedTextNode(
      String nodeId,
      String kind,
      Integer pageNumber,
      String text,
      Map<String, JsonNode> metadata) {}

  public record ParseDocumentResponse(
      UUID sourceVersionId,
      String sourceFormat,
      String parserVersion,
      JsonNode document,
      List<ParsedTextNode> textNodes,
      List<JsonNode> resources,
      List<JsonNode> warnings) {}

  public record SensitiveRuleDefinition(
      UUID id,
      String key,
      String name,
      String description,
      String dataType,
      String recognizerKind,
      List<String> locales,
      List<String> countryCodes,
      String regexPattern,
      String regexDialect,
      List<String> dictionaryTerms,
      String validatorName,
      BigDecimal confidenceThreshold,
      int priority,
      boolean enabled) {}

  public record SensitiveTextNode(
      String nodeId,
      String kind,
      Integer pageNumber,
      String text,
      Map<String, JsonNode> metadata) {}

  public record SensitiveTokenizationRequest(
      UUID sourceVersionId,
      String language,
      List<String> countryCodes,
      List<SensitiveRuleDefinition> rules,
      List<SensitiveTextNode> nodes) {}

  public record SensitiveTextSpan(String nodeId, int startOffset, int endOffset) {}

  public record SensitiveTokenReference(
      UUID id,
      UUID sourceVersionId,
      String token,
      String dataType,
      String maskedPreview,
      List<SensitiveTextSpan> occurrences) {}

  public record SensitiveDetection(
      String nodeId,
      int startOffset,
      int endOffset,
      String dataType,
      String countryCode,
      BigDecimal confidence,
      String ruleKey,
      String token) {}

  public record TokenizedSensitiveTextNode(
      String nodeId,
      String kind,
      Integer pageNumber,
      String tokenizedText,
      Map<String, JsonNode> metadata) {}

  public record SensitiveTokenizationResponse(
      UUID sourceVersionId,
      List<TokenizedSensitiveTextNode> nodes,
      List<SensitiveTokenReference> tokens,
      List<SensitiveDetection> detections) {}

  @JsonInclude(JsonInclude.Include.ALWAYS)
  public record SchemaFieldDefinition(
      UUID id,
      String key,
      String jsonPath,
      String description,
      String valueType,
      String arrayItemType,
      boolean required,
      boolean nullable,
      @JsonProperty("default") JsonNode defaultValue,
      String sensitivity,
      JsonNode constraints,
      JsonNode examples,
      String extractionHint,
      int position) {}

  public record TokenizedDocumentNode(
      String nodeId,
      String kind,
      Integer pageNumber,
      String tokenizedText,
      Map<String, JsonNode> metadata) {}

  public record TokenizedDocument(
      UUID sourceVersionId,
      String language,
      List<TokenizedDocumentNode> nodes) {}

  @JsonInclude(JsonInclude.Include.ALWAYS)
  public record AiExtractionRequest(
      UUID requestId,
      UUID jobId,
      UUID workspaceId,
      UUID extractionRunId,
      UUID sourceVersionId,
      UUID schemaVersionId,
      UUID sensitiveRuleTemplateVersionId,
      List<String> countryCodes,
      List<SchemaFieldDefinition> fields,
      JsonNode jsonSchema,
      List<SensitiveRuleDefinition> sensitiveRules,
      TokenizedDocument document) {}

  public record ModelEvidence(
      String nodeId,
      Integer pageNumber,
      String tokenizedText) {}

  public record ModelCandidateOutput(
      JsonNode value,
      BigDecimal confidence,
      List<ModelEvidence> evidence) {}

  public record ModelFieldOutput(
      String path,
      JsonNode value,
      BigDecimal confidence,
      List<ModelEvidence> evidence,
      List<ModelCandidateOutput> candidates,
      boolean needsReview) {}

  public record ModelExtractionOutput(
      JsonNode data,
      List<ModelFieldOutput> fields) {}

  public record ExtractionModelMetadata(
      String provider,
      String model,
      String promptVersion,
      Integer inputTokens,
      Integer outputTokens) {}

  public record AiExtractionResponse(
      UUID requestId,
      UUID jobId,
      UUID extractionRunId,
      ModelExtractionOutput result,
      ExtractionModelMetadata model,
      List<String> validationErrors) {}
}
