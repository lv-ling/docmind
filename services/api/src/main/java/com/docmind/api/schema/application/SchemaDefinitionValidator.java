package com.docmind.api.schema.application;

import com.docmind.api.schema.api.SchemaFieldConstraintsInput;
import com.docmind.api.schema.api.SchemaFieldInput;
import com.docmind.api.schema.domain.FieldSensitivity;
import com.docmind.api.schema.domain.SchemaFieldDefaultKind;
import com.docmind.api.schema.domain.SchemaValueType;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.shared.error.ApiFieldErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.re2j.Pattern;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SchemaDefinitionValidator {

  private static final java.util.regex.Pattern KEY_PATTERN =
      java.util.regex.Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
  private static final java.util.regex.Pattern JSON_PATH_PATTERN =
      java.util.regex.Pattern.compile(
          "^\\$(?:\\.[A-Za-z_][A-Za-z0-9_]*)+$");
  private static final java.util.regex.Pattern ROLE_KEY_PATTERN =
      java.util.regex.Pattern.compile("^[a-z][a-z0-9_-]{0,49}$");
  private static final Set<String> MASKS = Set.of("none", "partial", "full");

  private final ObjectMapper objectMapper;

  public SchemaDefinitionValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<ValidatedSchemaField> validate(List<SchemaFieldInput> fields) {
    List<ApiFieldErrorResponse> errors = new ArrayList<>();
    Set<String> keys = new HashSet<>();
    Set<String> jsonPaths = new HashSet<>();
    Set<Integer> positions = new HashSet<>();

    for (int index = 0; index < fields.size(); index++) {
      SchemaFieldInput field = fields.get(index);
      String path = "fields[" + index + "]";
      if (!KEY_PATTERN.matcher(field.key()).matches()) {
        error(errors, path + ".key", "invalid_key", "key 必须是字母或下划线开头的标识符");
      }
      if (!keys.add(field.key())) {
        error(errors, path + ".key", "duplicate", "同一 Schema 内 key 必须唯一");
      }
      if (!JSON_PATH_PATTERN.matcher(field.jsonPath()).matches()) {
        error(errors, path + ".json_path", "invalid_json_path", "仅支持以 $ 开头的确定性对象点路径");
      }
      if (!jsonPaths.add(field.jsonPath())) {
        error(errors, path + ".json_path", "duplicate", "同一 Schema 内 json_path 必须唯一");
      }
      if (!positions.add(field.position())) {
        error(errors, path + ".position", "duplicate", "字段位置必须唯一");
      }

      SchemaValueType valueType = parseEnum(SchemaValueType.class, field.valueType(), errors, path + ".value_type");
      SchemaValueType itemType =
          field.arrayItemType() == null
              ? null
              : parseEnum(
                  SchemaValueType.class,
                  field.arrayItemType(),
                  errors,
                  path + ".array_item_type");
      if (valueType == SchemaValueType.ARRAY && itemType == null) {
        error(errors, path + ".array_item_type", "required", "数组字段必须指定 array_item_type");
      }
      if (valueType != null && valueType != SchemaValueType.ARRAY && itemType != null) {
        error(errors, path + ".array_item_type", "not_allowed", "非数组字段不能指定 array_item_type");
      }

      SchemaFieldDefaultKind defaultKind =
          parseEnum(
              SchemaFieldDefaultKind.class,
              field.defaultValue().kind(),
              errors,
              path + ".default.kind");
      JsonNode literal = field.defaultValue().value();
      if (defaultKind == SchemaFieldDefaultKind.NONE && literal != null && !literal.isNull()) {
        error(errors, path + ".default.value", "not_allowed", "无默认值时不能提供 value");
      }
      if (defaultKind == SchemaFieldDefaultKind.LITERAL) {
        literal = literal == null ? NullNode.getInstance() : literal;
        validateValue(literal, valueType, itemType, field.nullable(), errors, path + ".default.value");
        validateLiteralConstraints(literal, field.constraints(), valueType, errors, path + ".default.value");
      }

      parseEnum(FieldSensitivity.class, field.sensitivity(), errors, path + ".sensitivity");
      validateConstraints(field, valueType, itemType, errors, path);
      for (int exampleIndex = 0; exampleIndex < field.examples().size(); exampleIndex++) {
        validateValue(
            field.examples().get(exampleIndex),
            valueType,
            itemType,
            field.nullable(),
            errors,
            path + ".examples[" + exampleIndex + "]");
      }
      if (!MASKS.contains(field.display().mask())) {
        error(errors, path + ".display.mask", "unsupported", "mask 必须是 none、partial 或 full");
      }
      Set<String> roleKeys = new HashSet<>();
      for (int roleIndex = 0; roleIndex < field.display().viewRoleKeys().size(); roleIndex++) {
        String roleKey = field.display().viewRoleKeys().get(roleIndex);
        if (!ROLE_KEY_PATTERN.matcher(roleKey).matches()) {
          error(
              errors,
              path + ".display.view_role_keys[" + roleIndex + "]",
              "invalid_role_key",
              "角色 key 格式无效");
        } else if (!roleKeys.add(roleKey)) {
          error(
              errors,
              path + ".display.view_role_keys[" + roleIndex + "]",
              "duplicate",
              "角色 key 不能重复");
        }
      }
      if (!field.metadata().isObject()) {
        error(errors, path + ".metadata", "type_mismatch", "metadata 必须是 JSON 对象");
      } else if (serializedSize(field.metadata()) > 16_384) {
        error(errors, path + ".metadata", "too_large", "metadata 不能超过 16 KiB");
      }
    }

    for (int expected = 0; expected < fields.size(); expected++) {
      if (!positions.contains(expected)) {
        error(errors, "fields", "invalid_positions", "position 必须从 0 开始连续排列");
        break;
      }
    }
    List<String> orderedPaths = jsonPaths.stream().sorted().toList();
    for (int index = 1; index < orderedPaths.size(); index++) {
      if (orderedPaths.get(index).startsWith(orderedPaths.get(index - 1) + ".")) {
        error(
            errors,
            "fields",
            "json_path_conflict",
            "字段 json_path 不能同时定义一个值及其子路径");
        break;
      }
    }
    if (!errors.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          ApiErrorCode.SCHEMA_INVALID,
          ApiErrorCategory.VALIDATION,
          "Schema 字段定义无效",
          java.util.Map.of("error_count", errors.size()),
          errors);
    }

    return fields.stream().map(this::validated).toList();
  }

  private ValidatedSchemaField validated(SchemaFieldInput field) {
    SchemaFieldDefaultKind defaultKind = enumValue(SchemaFieldDefaultKind.class, field.defaultValue().kind());
    JsonNode defaultValue =
        defaultKind == SchemaFieldDefaultKind.NONE
            ? null
            : field.defaultValue().value() == null
                ? NullNode.getInstance()
                : field.defaultValue().value();
    return new ValidatedSchemaField(
        field,
        enumValue(SchemaValueType.class, field.valueType()),
        field.arrayItemType() == null
            ? null
            : enumValue(SchemaValueType.class, field.arrayItemType()),
        defaultKind,
        defaultValue,
        enumValue(FieldSensitivity.class, field.sensitivity()),
        objectMapper.valueToTree(field.constraints()),
        objectMapper.valueToTree(field.examples()),
        objectMapper.valueToTree(field.display()),
        field.metadata().deepCopy());
  }

  private void validateConstraints(
      SchemaFieldInput field,
      SchemaValueType valueType,
      SchemaValueType itemType,
      List<ApiFieldErrorResponse> errors,
      String path) {
    SchemaFieldConstraintsInput constraints = field.constraints();
    if (constraints.format() != null
        && (constraints.format().isBlank() || constraints.format().length() > 100)) {
      error(errors, path + ".constraints.format", "invalid", "format 长度必须为 1 到 100 个字符");
    }
    if (constraints.format() != null
        && valueType != SchemaValueType.STRING
        && valueType != SchemaValueType.DATE
        && valueType != SchemaValueType.DATETIME) {
      error(errors, path + ".constraints.format", "not_applicable", "format 仅适用于字符串或日期时间字段");
    }
    if (valueType == SchemaValueType.DATE
        && constraints.format() != null
        && !"date".equals(constraints.format())) {
      error(errors, path + ".constraints.format", "unsupported", "date 字段的 format 只能是 date");
    }
    if (valueType == SchemaValueType.DATETIME
        && constraints.format() != null
        && !"date-time".equals(constraints.format())) {
      error(errors, path + ".constraints.format", "unsupported", "datetime 字段的 format 只能是 date-time");
    }
    if (constraints.pattern() != null) {
      if (valueType != SchemaValueType.STRING) {
        error(errors, path + ".constraints.pattern", "not_applicable", "pattern 仅适用于字符串字段");
      } else if (constraints.pattern().length() > 2000) {
        error(errors, path + ".constraints.pattern", "too_long", "pattern 不能超过 2000 个字符");
      } else {
        try {
          Pattern.compile(constraints.pattern());
        } catch (RuntimeException exception) {
          error(errors, path + ".constraints.pattern", "invalid_re2", "pattern 不是有效的 RE2 表达式");
        }
      }
    }
    if (constraints.enumValues().size() > 200) {
      error(errors, path + ".constraints.enum_values", "too_many", "枚举值不能超过 200 个");
    }
    for (int index = 0; index < constraints.enumValues().size(); index++) {
      validateValue(
          constraints.enumValues().get(index),
          valueType,
          itemType,
          field.nullable(),
          errors,
          path + ".constraints.enum_values[" + index + "]");
      for (int previous = 0; previous < index; previous++) {
        if (jsonEquals(constraints.enumValues().get(previous), constraints.enumValues().get(index))) {
          error(
              errors,
              path + ".constraints.enum_values[" + index + "]",
              "duplicate",
              "enum_values 不能包含重复值");
          break;
        }
      }
    }
    boolean lengthConstraint = constraints.minLength() != null || constraints.maxLength() != null;
    if (lengthConstraint && valueType != SchemaValueType.STRING && valueType != SchemaValueType.ARRAY) {
      error(errors, path + ".constraints", "not_applicable", "长度边界仅适用于字符串或数组字段");
    }
    if (constraints.minLength() != null && constraints.minLength() < 0
        || constraints.maxLength() != null && constraints.maxLength() < 0
        || constraints.minLength() != null
            && constraints.maxLength() != null
            && constraints.minLength() > constraints.maxLength()) {
      error(errors, path + ".constraints", "invalid_range", "长度边界必须为非负数且最小值不大于最大值");
    }
    boolean numericConstraint = constraints.minimum() != null || constraints.maximum() != null;
    if (numericConstraint && valueType != SchemaValueType.NUMBER && valueType != SchemaValueType.INTEGER) {
      error(errors, path + ".constraints", "not_applicable", "数值边界仅适用于 number 或 integer 字段");
    }
    if (constraints.minimum() != null
        && constraints.maximum() != null
        && constraints.minimum().compareTo(constraints.maximum()) > 0) {
      error(errors, path + ".constraints", "invalid_range", "minimum 不能大于 maximum");
    }
  }

  private void validateValue(
      JsonNode value,
      SchemaValueType valueType,
      SchemaValueType itemType,
      boolean nullable,
      List<ApiFieldErrorResponse> errors,
      String path) {
    if (value == null || value.isNull()) {
      if (!nullable) {
        error(errors, path, "null_not_allowed", "该字段不允许 null 字面量");
      }
      return;
    }
    if (valueType == null) {
      return;
    }
    boolean matches =
        switch (valueType) {
          case STRING -> value.isTextual();
          case NUMBER -> value.isNumber();
          case INTEGER ->
              value.isNumber() && value.decimalValue().stripTrailingZeros().scale() <= 0;
          case BOOLEAN -> value.isBoolean();
          case DATE -> value.isTextual() && validDate(value.textValue());
          case DATETIME -> value.isTextual() && validDateTime(value.textValue());
          case OBJECT -> value.isObject();
          case ARRAY -> value.isArray();
        };
    if (!matches) {
      error(errors, path, "type_mismatch", "值与字段类型 " + valueType.wireValue() + " 不匹配");
      return;
    }
    if (valueType == SchemaValueType.ARRAY && itemType != null) {
      for (int index = 0; index < value.size(); index++) {
        validateValue(value.get(index), itemType, null, false, errors, path + "[" + index + "]");
      }
    }
  }

  private void validateLiteralConstraints(
      JsonNode value,
      SchemaFieldConstraintsInput constraints,
      SchemaValueType valueType,
      List<ApiFieldErrorResponse> errors,
      String path) {
    if (value == null || value.isNull() || valueType == null) {
      return;
    }
    if (!constraints.enumValues().isEmpty()
        && constraints.enumValues().stream().noneMatch(candidate -> jsonEquals(candidate, value))) {
      error(errors, path, "enum_mismatch", "默认值不在 enum_values 中");
    }
    if (value.isTextual() && constraints.pattern() != null) {
      try {
        if (!Pattern.compile(constraints.pattern()).matcher(value.textValue()).find()) {
          error(errors, path, "pattern_mismatch", "默认值不满足 pattern");
        }
      } catch (RuntimeException ignored) {
        // The pattern itself is reported by validateConstraints.
      }
    }
    int length = value.isArray() ? value.size() : value.isTextual() ? value.textValue().length() : -1;
    if (length >= 0 && constraints.minLength() != null && length < constraints.minLength()) {
      error(errors, path, "minimum_length", "默认值短于 min_length");
    }
    if (length >= 0 && constraints.maxLength() != null && length > constraints.maxLength()) {
      error(errors, path, "maximum_length", "默认值长于 max_length");
    }
    if (value.isNumber()
        && constraints.minimum() != null
        && value.decimalValue().compareTo(constraints.minimum()) < 0) {
      error(errors, path, "minimum", "默认值小于 minimum");
    }
    if (value.isNumber()
        && constraints.maximum() != null
        && value.decimalValue().compareTo(constraints.maximum()) > 0) {
      error(errors, path, "maximum", "默认值大于 maximum");
    }
  }

  private boolean jsonEquals(JsonNode left, JsonNode right) {
    if (left.isNumber() && right.isNumber()) {
      return left.decimalValue().compareTo(right.decimalValue()) == 0;
    }
    return left.equals(right);
  }

  private boolean validDate(String value) {
    try {
      LocalDate.parse(value);
      return true;
    } catch (DateTimeParseException exception) {
      return false;
    }
  }

  private boolean validDateTime(String value) {
    try {
      OffsetDateTime.parse(value);
      return true;
    } catch (DateTimeParseException exception) {
      return false;
    }
  }

  private int serializedSize(JsonNode value) {
    try {
      return objectMapper.writeValueAsBytes(value).length;
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("JSON serialization failed", exception);
    }
  }

  private <E extends Enum<E>> E parseEnum(
      Class<E> type, String value, List<ApiFieldErrorResponse> errors, String path) {
    try {
      return enumValue(type, value);
    } catch (IllegalArgumentException exception) {
      error(errors, path, "unsupported", "不支持的值: " + value);
      return null;
    }
  }

  private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
    return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
  }

  private void error(
      List<ApiFieldErrorResponse> errors, String path, String code, String message) {
    errors.add(new ApiFieldErrorResponse(path, code, message));
  }
}
