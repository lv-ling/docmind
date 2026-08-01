package com.docmind.api.sensitive.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "sensitive_rule",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_sensitive_rule_key",
          columnNames = {"template_version_id", "rule_key"}),
      @UniqueConstraint(
          name = "uq_sensitive_rule_position",
          columnNames = {"template_version_id", "position"})
    })
public class SensitiveRule {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "template_version_id", nullable = false, updatable = false)
  private UUID templateVersionId;

  @Column(name = "rule_key", nullable = false, updatable = false, length = 64)
  private String key;

  @Column(nullable = false, updatable = false, length = 100)
  private String name;

  @Column(nullable = false, updatable = false, length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_type", nullable = false, updatable = false, length = 40)
  private SensitiveDataType dataType;

  @Enumerated(EnumType.STRING)
  @Column(name = "recognizer_kind", nullable = false, updatable = false, length = 20)
  private SensitiveRecognizerKind recognizerKind;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "locales_json", nullable = false, updatable = false)
  private List<String> locales;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "country_codes_json", nullable = false, updatable = false)
  private List<String> countryCodes;

  @Column(name = "regex_pattern", updatable = false, length = 2000)
  private String regexPattern;

  @Column(name = "regex_dialect", updatable = false, length = 20)
  private String regexDialect;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dictionary_terms_json", nullable = false, updatable = false)
  private List<String> dictionaryTerms;

  @Column(name = "validator_name", updatable = false, length = 100)
  private String validatorName;

  @Column(name = "confidence_threshold", nullable = false, updatable = false, precision = 4, scale = 3)
  private BigDecimal confidenceThreshold;

  @Column(nullable = false, updatable = false)
  private int priority;

  @Column(nullable = false, updatable = false)
  private boolean enabled;

  @Column(nullable = false, updatable = false)
  private int position;

  protected SensitiveRule() {}

  public SensitiveRule(
      UUID templateVersionId,
      String key,
      String name,
      String description,
      SensitiveDataType dataType,
      SensitiveRecognizerKind recognizerKind,
      List<String> locales,
      List<String> countryCodes,
      String regexPattern,
      String regexDialect,
      List<String> dictionaryTerms,
      String validatorName,
      BigDecimal confidenceThreshold,
      int priority,
      boolean enabled,
      int position) {
    this.templateVersionId = templateVersionId;
    this.key = key;
    this.name = name;
    this.description = description;
    this.dataType = dataType;
    this.recognizerKind = recognizerKind;
    this.locales = List.copyOf(locales);
    this.countryCodes = List.copyOf(countryCodes);
    this.regexPattern = regexPattern;
    this.regexDialect = regexDialect;
    this.dictionaryTerms = List.copyOf(dictionaryTerms);
    this.validatorName = validatorName;
    this.confidenceThreshold = confidenceThreshold;
    this.priority = priority;
    this.enabled = enabled;
    this.position = position;
  }

  public UUID id() { return id; }
  public UUID templateVersionId() { return templateVersionId; }
  public String key() { return key; }
  public String name() { return name; }
  public String description() { return description; }
  public SensitiveDataType dataType() { return dataType; }
  public SensitiveRecognizerKind recognizerKind() { return recognizerKind; }
  public List<String> locales() { return List.copyOf(locales); }
  public List<String> countryCodes() { return List.copyOf(countryCodes); }
  public String regexPattern() { return regexPattern; }
  public String regexDialect() { return regexDialect; }
  public List<String> dictionaryTerms() { return List.copyOf(dictionaryTerms); }
  public String validatorName() { return validatorName; }
  public BigDecimal confidenceThreshold() { return confidenceThreshold; }
  public int priority() { return priority; }
  public boolean enabled() { return enabled; }
  public int position() { return position; }
}
