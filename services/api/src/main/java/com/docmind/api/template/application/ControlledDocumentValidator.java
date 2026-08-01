package com.docmind.api.template.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ControlledDocumentValidator {
  private static final int MAX_NODES = 100_000;
  private static final int MAX_DEPTH = 100;
  private static final int MAX_BYTES = 5 * 1024 * 1024;
  private static final Set<String> BLOCK_TYPES =
      Set.of(
          "paragraph",
          "heading",
          "list",
          "table",
          "image",
          "table_of_contents",
          "page_break",
          "page_marker",
          "template_repeat");
  private static final Set<String> INLINE_TYPES =
      Set.of("text", "line_break", "tab", "dynamic_field", "template_placeholder");

  private final ObjectMapper objectMapper;

  public ControlledDocumentValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void validate(JsonNode document) {
    if (document == null
        || !document.isObject()
        || !"1.0".equals(document.path("model_version").asText())
        || blank(document.path("root_id").asText())
        || !document.path("metadata").isObject()
        || !document.path("page_layout").isObject()
        || !document.path("headers").isArray()
        || !document.path("footers").isArray()
        || !document.path("blocks").isArray()) {
      throw invalid();
    }
    try {
      if (objectMapper.writeValueAsBytes(document).length > MAX_BYTES) throw invalid();
    } catch (JsonProcessingException exception) {
      throw new TemplateConversionException("CONTROLLED_DOCUMENT_INVALID", false, exception);
    }
    Counter counter = new Counter();
    Set<String> ids = new HashSet<>();
    validateRegions(document.path("headers"), 0, counter, ids);
    validateRegions(document.path("footers"), 0, counter, ids);
    validateBlocks(document.path("blocks"), 0, counter, ids);
  }

  private void validateRegions(JsonNode regions, int depth, Counter counter, Set<String> ids) {
    for (JsonNode region : regions) {
      if (!region.isObject()
          || !Set.of("default", "first_page", "even_pages")
              .contains(region.path("variant").asText())
          || !region.path("blocks").isArray()) throw invalid();
      validateBlocks(region.path("blocks"), depth + 1, counter, ids);
    }
  }

  private void validateBlocks(JsonNode blocks, int depth, Counter counter, Set<String> ids) {
    requireDepth(depth);
    for (JsonNode block : blocks) {
      requireNode(block, BLOCK_TYPES, counter, ids);
      String type = block.path("type").asText();
      switch (type) {
        case "paragraph", "heading" -> validateInline(block.path("content"), depth + 1, counter, ids);
        case "list" -> {
          if (!block.path("items").isArray()) throw invalid();
          for (JsonNode item : block.path("items")) {
            requireId(item, counter, ids);
            if (!item.path("blocks").isArray()) throw invalid();
            validateBlocks(item.path("blocks"), depth + 1, counter, ids);
          }
        }
        case "table" -> {
          if (!block.path("rows").isArray()) throw invalid();
          for (JsonNode row : block.path("rows")) {
            requireId(row, counter, ids);
            if (!row.path("cells").isArray()) throw invalid();
            for (JsonNode cell : row.path("cells")) {
              requireId(cell, counter, ids);
              if (!cell.path("blocks").isArray()) throw invalid();
              validateBlocks(cell.path("blocks"), depth + 1, counter, ids);
            }
          }
        }
        case "template_repeat" -> {
          if (!block.path("blocks").isArray() || !block.path("binding").isObject()) throw invalid();
          validateBlocks(block.path("blocks"), depth + 1, counter, ids);
        }
        default -> {
          // Leaf block types have no nested editable nodes.
        }
      }
    }
  }

  private void validateInline(JsonNode content, int depth, Counter counter, Set<String> ids) {
    requireDepth(depth);
    if (!content.isArray()) throw invalid();
    for (JsonNode item : content) {
      requireNode(item, INLINE_TYPES, counter, ids);
      if ("text".equals(item.path("type").asText()) && !item.path("text").isTextual()) {
        throw invalid();
      }
      if ("template_placeholder".equals(item.path("type").asText())
          && !item.path("binding").isObject()) throw invalid();
    }
  }

  private void requireNode(
      JsonNode node, Set<String> allowed, Counter counter, Set<String> ids) {
    if (!node.isObject() || !allowed.contains(node.path("type").asText())) throw invalid();
    requireId(node, counter, ids);
    if (!node.path("attributes").isObject()) throw invalid();
  }

  private void requireId(JsonNode node, Counter counter, Set<String> ids) {
    String id = node.path("id").asText();
    counter.value++;
    if (counter.value > MAX_NODES || blank(id) || id.length() > 255 || !ids.add(id)) {
      throw invalid();
    }
  }

  private void requireDepth(int depth) {
    if (depth > MAX_DEPTH) throw invalid();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private TemplateConversionException invalid() {
    return new TemplateConversionException("CONTROLLED_DOCUMENT_INVALID", false);
  }

  private static final class Counter {
    private int value;
  }
}
