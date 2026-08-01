package com.docmind.api.extraction.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SensitiveTokenRestorer {

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("\\[\\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}]]");
  private static final String TOKEN_PREFIX = "[[SENSITIVE:";

  private final ObjectMapper objectMapper;

  public SensitiveTokenRestorer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public JsonNode restore(JsonNode input, Map<String, String> replacements) {
    if (input == null) {
      throw new ExtractionResultValidationException("MODEL_VALUE_MISSING");
    }
    if (input.isTextual()) {
      return TextNode.valueOf(restoreText(input.textValue(), replacements));
    }
    if (input.isArray()) {
      ArrayNode result = objectMapper.createArrayNode();
      input.forEach(item -> result.add(restore(item, replacements)));
      return result;
    }
    if (input.isObject()) {
      ObjectNode result = objectMapper.createObjectNode();
      Iterator<Map.Entry<String, JsonNode>> fields = input.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        result.set(field.getKey(), restore(field.getValue(), replacements));
      }
      return result;
    }
    return input.deepCopy();
  }

  public String restoreText(String input, Map<String, String> replacements) {
    Matcher matcher = TOKEN_PATTERN.matcher(input);
    StringBuilder output = new StringBuilder(input.length());
    int cursor = 0;
    while (matcher.find()) {
      String replacement = replacements.get(matcher.group());
      if (replacement == null) {
        throw new ExtractionResultValidationException("SENSITIVE_TOKEN_MISSING");
      }
      output.append(input, cursor, matcher.start());
      output.append(replacement);
      cursor = matcher.end();
    }
    output.append(input, cursor, input.length());
    if (output.indexOf(TOKEN_PREFIX) >= 0 && input.indexOf(TOKEN_PREFIX) >= 0) {
      throw new ExtractionResultValidationException("SENSITIVE_TOKEN_MALFORMED");
    }
    return output.toString();
  }

  public boolean containsKnownToken(JsonNode input, Map<String, String> replacements) {
    if (input == null) {
      return false;
    }
    if (input.isTextual()) {
      Matcher matcher = TOKEN_PATTERN.matcher(input.textValue());
      while (matcher.find()) {
        if (replacements.containsKey(matcher.group())) {
          return true;
        }
      }
      return false;
    }
    if (input.isContainerNode()) {
      for (JsonNode child : input) {
        if (containsKnownToken(child, replacements)) {
          return true;
        }
      }
    }
    return false;
  }
}
