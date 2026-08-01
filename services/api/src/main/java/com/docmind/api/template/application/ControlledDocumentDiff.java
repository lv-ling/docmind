package com.docmind.api.template.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ControlledDocumentDiff {
  private static final int MAX_CHANGES = 10_000;
  private final ObjectMapper objectMapper;

  public ControlledDocumentDiff(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ObjectNode compare(JsonNode before, JsonNode after) {
    ArrayNode changes = objectMapper.createArrayNode();
    walk("$", before, after, changes);
    ObjectNode result = objectMapper.createObjectNode();
    result.put("version", "1.0");
    result.put("truncated", changes.size() >= MAX_CHANGES);
    result.set("changes", changes);
    return result;
  }

  private void walk(String path, JsonNode before, JsonNode after, ArrayNode changes) {
    if (changes.size() >= MAX_CHANGES || java.util.Objects.equals(before, after)) return;
    if (before == null || before.isMissingNode()) {
      add(changes, path, "added", null, after);
      return;
    }
    if (after == null || after.isMissingNode()) {
      add(changes, path, "removed", before, null);
      return;
    }
    if (before.isObject() && after.isObject()) {
      Set<String> names = new LinkedHashSet<>();
      before.fieldNames().forEachRemaining(names::add);
      after.fieldNames().forEachRemaining(names::add);
      names.forEach(name -> walk(path + "." + name, before.get(name), after.get(name), changes));
      return;
    }
    if (before.isArray() && after.isArray()) {
      int size = Math.max(before.size(), after.size());
      for (int index = 0; index < size && changes.size() < MAX_CHANGES; index++) {
        walk(path + "[" + index + "]", before.path(index), after.path(index), changes);
      }
      return;
    }
    add(changes, path, "changed", before, after);
  }

  private void add(ArrayNode changes, String path, String kind, JsonNode before, JsonNode after) {
    ObjectNode change = changes.addObject();
    change.put("path", path);
    change.put("kind", kind);
    change.set("before", before == null ? objectMapper.nullNode() : before.deepCopy());
    change.set("after", after == null ? objectMapper.nullNode() : after.deepCopy());
  }
}
