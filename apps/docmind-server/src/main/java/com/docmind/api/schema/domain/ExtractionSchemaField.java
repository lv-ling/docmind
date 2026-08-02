package com.docmind.api.schema.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "extraction_schema_field",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_extraction_schema_field_key",
          columnNames = {"schema_version_id", "field_key"}),
      @UniqueConstraint(
          name = "uq_extraction_schema_field_path",
          columnNames = {"schema_version_id", "json_path"}),
      @UniqueConstraint(
          name = "uq_extraction_schema_field_position",
          columnNames = {"schema_version_id", "position"})
    })
public class ExtractionSchemaField {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "schema_version_id", nullable = false, updatable = false)
  private UUID schemaVersionId;

  @Column(name = "field_key", nullable = false, updatable = false, length = 64)
  private String key;

  @Column(name = "json_path", nullable = false, updatable = false, length = 500)
  private String jsonPath;

  @Column(nullable = false, updatable = false, length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_type", nullable = false, updatable = false, length = 20)
  private SchemaValueType valueType;

  @Enumerated(EnumType.STRING)
  @Column(name = "array_item_type", updatable = false, length = 20)
  private SchemaValueType arrayItemType;

  @Column(name = "is_required", nullable = false, updatable = false)
  private boolean required;

  @Column(nullable = false, updatable = false)
  private boolean nullable;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_kind", nullable = false, updatable = false, length = 20)
  private SchemaFieldDefaultKind defaultKind;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "default_value", updatable = false)
  private JsonNode defaultValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private FieldSensitivity sensitivity;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "constraints_json", nullable = false, updatable = false)
  private JsonNode constraints;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "examples_json", nullable = false, updatable = false)
  private JsonNode examples;

  @Column(name = "extraction_hint", updatable = false, length = 2000)
  private String extractionHint;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "display_json", nullable = false, updatable = false)
  private JsonNode display;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", nullable = false, updatable = false)
  private JsonNode metadata;

  @Column(nullable = false, updatable = false)
  private int position;

  protected ExtractionSchemaField() {}

  public ExtractionSchemaField(
      UUID schemaVersionId,
      String key,
      String jsonPath,
      String description,
      SchemaValueType valueType,
      SchemaValueType arrayItemType,
      boolean required,
      boolean nullable,
      SchemaFieldDefaultKind defaultKind,
      JsonNode defaultValue,
      FieldSensitivity sensitivity,
      JsonNode constraints,
      JsonNode examples,
      String extractionHint,
      JsonNode display,
      JsonNode metadata,
      int position) {
    this.schemaVersionId = schemaVersionId;
    this.key = key;
    this.jsonPath = jsonPath;
    this.description = description;
    this.valueType = valueType;
    this.arrayItemType = arrayItemType;
    this.required = required;
    this.nullable = nullable;
    this.defaultKind = defaultKind;
    this.defaultValue = defaultValue == null ? null : defaultValue.deepCopy();
    this.sensitivity = sensitivity;
    this.constraints = constraints.deepCopy();
    this.examples = examples.deepCopy();
    this.extractionHint = extractionHint;
    this.display = display.deepCopy();
    this.metadata = metadata.deepCopy();
    this.position = position;
  }

  public UUID id() { return id; }
  public UUID schemaVersionId() { return schemaVersionId; }
  public String key() { return key; }
  public String jsonPath() { return jsonPath; }
  public String description() { return description; }
  public SchemaValueType valueType() { return valueType; }
  public SchemaValueType arrayItemType() { return arrayItemType; }
  public boolean required() { return required; }
  public boolean nullable() { return nullable; }
  public SchemaFieldDefaultKind defaultKind() { return defaultKind; }
  public JsonNode defaultValue() { return defaultValue == null ? null : defaultValue.deepCopy(); }
  public FieldSensitivity sensitivity() { return sensitivity; }
  public JsonNode constraints() { return constraints.deepCopy(); }
  public JsonNode examples() { return examples.deepCopy(); }
  public String extractionHint() { return extractionHint; }
  public JsonNode display() { return display.deepCopy(); }
  public JsonNode metadata() { return metadata.deepCopy(); }
  public int position() { return position; }
}
