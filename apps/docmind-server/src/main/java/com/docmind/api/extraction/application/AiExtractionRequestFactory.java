package com.docmind.api.extraction.application;

import com.docmind.api.extraction.ai.AiServiceContracts.AiExtractionRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.SchemaFieldDefinition;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveRuleDefinition;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTextNode;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationRequest;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.TokenizedDocument;
import com.docmind.api.extraction.ai.AiServiceContracts.TokenizedDocumentNode;
import com.docmind.api.schema.domain.ExtractionSchemaField;
import com.docmind.api.schema.domain.SchemaFieldDefaultKind;
import com.docmind.api.sensitive.domain.SensitiveRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiExtractionRequestFactory {

  public static final List<String> SUPPORTED_COUNTRIES =
      List.of("CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL");

  private final ObjectMapper objectMapper;

  public AiExtractionRequestFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public SensitiveTokenizationRequest tokenizationRequest(
      ExtractionWorkItem item, ParseDocumentResponse parsed) {
    return new SensitiveTokenizationRequest(
        item.source().id(),
        language(parsed),
        SUPPORTED_COUNTRIES,
        item.sensitiveRules().stream().map(this::sensitiveRule).toList(),
        parsed.textNodes().stream()
            .map(
                node ->
                    new SensitiveTextNode(
                        node.nodeId(),
                        node.kind(),
                        node.pageNumber(),
                        node.text(),
                        node.metadata()))
            .toList());
  }

  public AiExtractionRequest extractionRequest(
      ExtractionWorkItem item,
      ParseDocumentResponse parsed,
      SensitiveTokenizationResponse tokenized,
      UUID requestId) {
    requireTokenizedNodesAlign(parsed, tokenized);
    return new AiExtractionRequest(
        requestId,
        item.run().jobId(),
        item.run().workspaceId(),
        item.run().id(),
        item.source().id(),
        item.schemaVersion().id(),
        item.run().sensitiveRuleTemplateVersionId(),
        SUPPORTED_COUNTRIES,
        item.fields().stream().map(this::schemaField).toList(),
        item.schemaVersion().jsonSchema(),
        item.sensitiveRules().stream().map(this::sensitiveRule).toList(),
        new TokenizedDocument(
            item.source().id(),
            language(parsed),
            tokenized.nodes().stream()
                .map(
                    node ->
                        new TokenizedDocumentNode(
                            node.nodeId(),
                            node.kind(),
                            node.pageNumber(),
                            node.tokenizedText(),
                            node.metadata()))
                .toList()));
  }

  private SchemaFieldDefinition schemaField(ExtractionSchemaField field) {
    ObjectNode defaultValue = objectMapper.createObjectNode();
    defaultValue.put("kind", field.defaultKind().wireValue());
    if (field.defaultKind() == SchemaFieldDefaultKind.LITERAL) {
      defaultValue.set("value", field.defaultValue());
    }
    return new SchemaFieldDefinition(
        field.id(),
        field.key(),
        field.jsonPath(),
        field.description(),
        field.valueType().wireValue(),
        field.arrayItemType() == null ? null : field.arrayItemType().wireValue(),
        field.required(),
        field.nullable(),
        defaultValue,
        field.sensitivity().wireValue(),
        field.constraints(),
        field.examples(),
        field.extractionHint(),
        field.position());
  }

  private SensitiveRuleDefinition sensitiveRule(SensitiveRule rule) {
    return new SensitiveRuleDefinition(
        rule.id(),
        rule.key(),
        rule.name(),
        rule.description(),
        rule.dataType().wireValue(),
        rule.recognizerKind().wireValue(),
        rule.locales(),
        rule.countryCodes(),
        rule.regexPattern(),
        rule.regexDialect(),
        rule.dictionaryTerms(),
        rule.validatorName(),
        rule.confidenceThreshold(),
        rule.priority(),
        rule.enabled());
  }

  private String language(ParseDocumentResponse parsed) {
    JsonNode language = parsed.document().path("metadata").path("language");
    return language.isTextual() && !language.textValue().isBlank() ? language.textValue() : "und";
  }

  private void requireTokenizedNodesAlign(
      ParseDocumentResponse parsed, SensitiveTokenizationResponse tokenized) {
    if (tokenized.nodes() == null || parsed.textNodes().size() != tokenized.nodes().size()) {
      throw new ExtractionResultValidationException("TOKENIZED_NODE_SET_MISMATCH");
    }
    Set<String> parsedIds = new HashSet<>();
    parsed.textNodes().forEach(node -> parsedIds.add(node.nodeId()));
    Set<String> tokenizedIds = new HashSet<>();
    tokenized.nodes().forEach(
        node -> {
          if (node == null
              || node.nodeId() == null
              || node.kind() == null
              || node.tokenizedText() == null
              || !tokenizedIds.add(node.nodeId())) {
            throw new ExtractionResultValidationException("TOKENIZED_NODE_INVALID");
          }
        });
    if (!parsedIds.equals(tokenizedIds)) {
      throw new ExtractionResultValidationException("TOKENIZED_NODE_SET_MISMATCH");
    }
  }
}
