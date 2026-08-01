package com.docmind.api.schema.application;

import com.docmind.api.schema.api.SchemaFieldConstraintsInput;
import com.docmind.api.schema.domain.SchemaFieldDefaultKind;
import com.docmind.api.schema.domain.SchemaValueType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SchemaJsonSchemaFactory {

  private final ObjectMapper objectMapper;

  public SchemaJsonSchemaFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ObjectNode create(List<ValidatedSchemaField> fields) {
    ObjectNode schema = objectMapper.createObjectNode();
    schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    schema.put("type", "object");
    schema.put("additionalProperties", false);
    schema.putObject("properties");

    for (ValidatedSchemaField field : fields) {
      String[] path = field.input().jsonPath().substring(2).split("\\.");
      ObjectNode parent = schema;
      ObjectNode properties = (ObjectNode) parent.get("properties");
      for (int index = 0; index < path.length - 1; index++) {
        JsonNode existing = properties.get(path[index]);
        ObjectNode child;
        if (existing == null) {
          child = properties.putObject(path[index]);
          child.put("type", "object");
          child.put("additionalProperties", false);
          child.putObject("properties");
        } else {
          child = (ObjectNode) existing;
        }
        if (field.input().required()) {
          addRequired(parent, path[index]);
        }
        parent = child;
        properties = (ObjectNode) child.get("properties");
      }
      String propertyName = path[path.length - 1];
      ObjectNode property = properties.putObject(propertyName);
      boolean missingWithoutDefault = field.defaultKind() == SchemaFieldDefaultKind.NONE;
      putType(property, field.valueType(), field.input().nullable() || missingWithoutDefault);
      property.put("description", field.input().description());
      property.put("x-docmind-json-path", field.input().jsonPath());
      property.put("x-docmind-field-key", field.input().key());
      property.put("x-docmind-sensitivity", field.sensitivity().wireValue());
      if (missingWithoutDefault) {
        property.put("x-docmind-null-policy", "missing_without_default");
      }
      if (field.valueType() == SchemaValueType.ARRAY) {
        ObjectNode items = property.putObject("items");
        putType(items, field.arrayItemType(), false);
      }
      if (field.valueType() == SchemaValueType.DATE) {
        property.put("format", "date");
      } else if (field.valueType() == SchemaValueType.DATETIME) {
        property.put("format", "date-time");
      }
      applyConstraints(
          property, field.valueType(), field.input().constraints(), missingWithoutDefault);
      if (field.defaultKind() == SchemaFieldDefaultKind.LITERAL) {
        property.set("default", field.defaultValue());
      }
      if (field.input().required()) {
        addRequired(parent, propertyName);
      }
    }
    return schema;
  }

  private void addRequired(ObjectNode parent, String propertyName) {
    ArrayNode required =
        parent.has("required") ? (ArrayNode) parent.get("required") : parent.putArray("required");
    for (JsonNode existing : required) {
      if (propertyName.equals(existing.textValue())) {
        return;
      }
    }
    required.add(propertyName);
  }

  private void putType(ObjectNode target, SchemaValueType valueType, boolean nullable) {
    String type = jsonType(valueType);
    if (nullable) {
      ArrayNode types = target.putArray("type");
      types.add(type);
      types.add("null");
    } else {
      target.put("type", type);
    }
  }

  private String jsonType(SchemaValueType valueType) {
    return switch (valueType) {
      case DATE, DATETIME -> "string";
      default -> valueType.wireValue();
    };
  }

  private void applyConstraints(
      ObjectNode property,
      SchemaValueType valueType,
      SchemaFieldConstraintsInput constraints,
      boolean allowMissingNull) {
    if (constraints.format() != null) {
      property.put("format", constraints.format());
    }
    if (constraints.pattern() != null) {
      property.put("pattern", constraints.pattern());
    }
    if (!constraints.enumValues().isEmpty()) {
      ArrayNode values = property.putArray("enum");
      constraints.enumValues().forEach(values::add);
      if (allowMissingNull && constraints.enumValues().stream().noneMatch(JsonNode::isNull)) {
        values.addNull();
      }
    }
    if (constraints.minLength() != null) {
      property.put(valueType == SchemaValueType.ARRAY ? "minItems" : "minLength", constraints.minLength());
    }
    if (constraints.maxLength() != null) {
      property.put(valueType == SchemaValueType.ARRAY ? "maxItems" : "maxLength", constraints.maxLength());
    }
    if (constraints.minimum() != null) {
      property.put("minimum", constraints.minimum());
    }
    if (constraints.maximum() != null) {
      property.put("maximum", constraints.maximum());
    }
  }
}
