package com.docmind.api.extraction.application;

import com.docmind.api.extraction.ai.AiServiceContracts.ParseDocumentResponse;
import com.docmind.api.extraction.ai.AiServiceContracts.ParsedTextNode;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTextSpan;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenReference;
import com.docmind.api.extraction.ai.AiServiceContracts.SensitiveTokenizationResponse;
import com.docmind.api.extraction.domain.SensitiveToken;
import com.docmind.api.extraction.infrastructure.SensitiveTokenRepository;
import com.docmind.api.infrastructure.crypto.JsonEnvelopeEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveTokenMappingService {

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("\\[\\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}]]");

  private final SensitiveTokenRepository tokens;
  private final JsonEnvelopeEncryption encryption;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public SensitiveTokenMappingService(
      SensitiveTokenRepository tokens,
      JsonEnvelopeEncryption encryption,
      ObjectMapper objectMapper) {
    this(tokens, encryption, objectMapper, Clock.systemUTC());
  }

  SensitiveTokenMappingService(
      SensitiveTokenRepository tokens,
      JsonEnvelopeEncryption encryption,
      ObjectMapper objectMapper,
      Clock clock) {
    this.tokens = tokens;
    this.encryption = encryption;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public SensitiveTokenMapping build(
      UUID extractionRunId,
      UUID sourceVersionId,
      ParseDocumentResponse parsed,
      SensitiveTokenizationResponse tokenized) {
    if (!sourceVersionId.equals(parsed.sourceVersionId())
        || !sourceVersionId.equals(tokenized.sourceVersionId())
        || parsed.textNodes() == null
        || tokenized.tokens() == null) {
      throw new ExtractionResultValidationException("SENSITIVE_TOKEN_RESPONSE_INVALID");
    }
    Map<String, ParsedTextNode> nodes = new HashMap<>();
    for (ParsedTextNode node : parsed.textNodes()) {
      if (node == null || node.nodeId() == null || nodes.put(node.nodeId(), node) != null) {
        throw new ExtractionResultValidationException("PARSED_NODE_ID_INVALID");
      }
    }

    List<SensitiveToken> entities = new ArrayList<>();
    Map<String, String> replacements = new LinkedHashMap<>();
    Set<String> ambiguities = new HashSet<>();
    Set<String> seenTokens = new HashSet<>();
    Instant now = clock.instant();
    for (SensitiveTokenReference reference : tokenized.tokens()) {
      requireReference(reference, sourceVersionId, seenTokens);
      ArrayNode occurrencePayload = objectMapper.createArrayNode();
      String selectedValue = null;
      for (SensitiveTextSpan occurrence : reference.occurrences()) {
        ParsedTextNode node = nodes.get(occurrence.nodeId());
        String original = extractCodePointRange(node, occurrence);
        if (selectedValue == null) {
          selectedValue = original;
        } else if (!selectedValue.equals(original)) {
          ambiguities.add(reference.token());
        }
        ObjectNode item = occurrencePayload.addObject();
        item.put("node_id", occurrence.nodeId());
        item.put("start_offset", occurrence.startOffset());
        item.put("end_offset", occurrence.endOffset());
        item.put("original_value", original);
      }
      if (selectedValue == null || selectedValue.isEmpty()) {
        throw new ExtractionResultValidationException("SENSITIVE_TOKEN_OCCURRENCE_INVALID");
      }
      replacements.put(reference.token(), selectedValue);
      ObjectNode plaintext = objectMapper.createObjectNode();
      plaintext.put("token_reference_id", reference.id().toString());
      plaintext.set("occurrences", occurrencePayload);
      String context = context(extractionRunId, reference.token());
      entities.add(
          new SensitiveToken(
              UUID.randomUUID(),
              extractionRunId,
              sourceVersionId,
              reference.token(),
              reference.dataType(),
              encryption.encrypt(plaintext, context),
              safeMaskedPreview(reference.maskedPreview()),
              now));
    }
    return new SensitiveTokenMapping(entities, replacements, ambiguities);
  }

  @Transactional
  public void replacePersisted(UUID extractionRunId, SensitiveTokenMapping mapping) {
    tokens.deleteAllByExtractionRunId(extractionRunId);
    tokens.flush();
    tokens.saveAll(mapping.entities());
    tokens.flush();
  }

  private void requireReference(
      SensitiveTokenReference reference, UUID sourceVersionId, Set<String> seenTokens) {
    if (reference == null
        || reference.id() == null
        || !sourceVersionId.equals(reference.sourceVersionId())
        || reference.token() == null
        || !TOKEN_PATTERN.matcher(reference.token()).matches()
        || !seenTokens.add(reference.token())
        || reference.dataType() == null
        || reference.dataType().isBlank()
        || reference.occurrences() == null
        || reference.occurrences().isEmpty()) {
      throw new ExtractionResultValidationException("SENSITIVE_TOKEN_REFERENCE_INVALID");
    }
  }

  private String extractCodePointRange(ParsedTextNode node, SensitiveTextSpan occurrence) {
    if (node == null || node.text() == null || occurrence == null) {
      throw new ExtractionResultValidationException("SENSITIVE_TOKEN_OCCURRENCE_INVALID");
    }
    int codePoints = node.text().codePointCount(0, node.text().length());
    if (occurrence.startOffset() < 0
        || occurrence.endOffset() <= occurrence.startOffset()
        || occurrence.endOffset() > codePoints) {
      throw new ExtractionResultValidationException("SENSITIVE_TOKEN_OCCURRENCE_INVALID");
    }
    int start = node.text().offsetByCodePoints(0, occurrence.startOffset());
    int end = node.text().offsetByCodePoints(0, occurrence.endOffset());
    return node.text().substring(start, end);
  }

  private String safeMaskedPreview(String preview) {
    if (preview == null || preview.isBlank() || preview.length() > 100) {
      return "[敏感内容]";
    }
    return preview;
  }

  private String context(UUID extractionRunId, String token) {
    return "sensitive-token:" + extractionRunId + ":" + token;
  }
}
