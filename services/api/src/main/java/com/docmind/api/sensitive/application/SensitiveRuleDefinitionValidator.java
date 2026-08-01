package com.docmind.api.sensitive.application;

import com.docmind.api.sensitive.api.SensitiveRuleInput;
import com.docmind.api.sensitive.domain.SensitiveDataType;
import com.docmind.api.sensitive.domain.SensitiveRecognizerKind;
import com.docmind.api.shared.error.ApiErrorCategory;
import com.docmind.api.shared.error.ApiErrorCode;
import com.docmind.api.shared.error.ApiException;
import com.docmind.api.shared.error.ApiFieldErrorResponse;
import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SensitiveRuleDefinitionValidator {

  private static final java.util.regex.Pattern KEY_PATTERN =
      java.util.regex.Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,63}$");
  private static final java.util.regex.Pattern LOCALE_PATTERN =
      java.util.regex.Pattern.compile("^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*$");
  private static final java.util.regex.Pattern COUNTRY_PATTERN =
      java.util.regex.Pattern.compile("^[A-Z]{2}$");
  private static final Set<String> VALIDATOR_NAMES =
      Set.of(
          "cn_resident_identity",
          "e164_phone",
          "email",
          "luhn",
          "iban",
          "ip_address",
          "passport_document");
  private static final Set<String> SUPPORTED_COUNTRIES =
      Set.of("CN", "US", "JP", "KR", "DE", "FR", "GB", "AU", "NL");

  public List<ValidatedSensitiveRule> validate(List<SensitiveRuleInput> rules) {
    List<ApiFieldErrorResponse> errors = new ArrayList<>();
    Set<String> keys = new HashSet<>();

    for (int index = 0; index < rules.size(); index++) {
      SensitiveRuleInput rule = rules.get(index);
      String path = "rules[" + index + "]";
      if (!KEY_PATTERN.matcher(rule.key()).matches()) {
        error(errors, path + ".key", "invalid_key", "规则 key 格式无效");
      } else if (!keys.add(rule.key())) {
        error(errors, path + ".key", "duplicate", "同一版本内规则 key 必须唯一");
      }

      SensitiveDataType dataType =
          parseEnum(SensitiveDataType.class, rule.dataType(), errors, path + ".data_type");
      SensitiveRecognizerKind kind =
          parseEnum(
              SensitiveRecognizerKind.class,
              rule.recognizerKind(),
              errors,
              path + ".recognizer_kind");

      validateLocales(rule.locales(), errors, path + ".locales");
      validateCountries(rule.countryCodes(), errors, path + ".country_codes");
      validateRecognizerConfiguration(rule, dataType, kind, errors, path);
    }

    if (!errors.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          ApiErrorCode.VALIDATION_FAILED,
          ApiErrorCategory.VALIDATION,
          "敏感规则定义无效",
          java.util.Map.of("error_count", errors.size()),
          errors);
    }

    List<ValidatedSensitiveRule> validated = new ArrayList<>();
    for (int index = 0; index < rules.size(); index++) {
      SensitiveRuleInput rule = rules.get(index);
      validated.add(
          new ValidatedSensitiveRule(
              rule,
              enumValue(SensitiveDataType.class, rule.dataType()),
              enumValue(SensitiveRecognizerKind.class, rule.recognizerKind()),
              index));
    }
    return List.copyOf(validated);
  }

  private void validateLocales(
      List<String> locales, List<ApiFieldErrorResponse> errors, String path) {
    Set<String> values = new HashSet<>();
    for (int index = 0; index < locales.size(); index++) {
      String locale = locales.get(index);
      if (!LOCALE_PATTERN.matcher(locale).matches()) {
        error(errors, path + "[" + index + "]", "invalid_locale", "locale 必须是 BCP 47 风格标签");
      } else if (!values.add(locale.toLowerCase(Locale.ROOT))) {
        error(errors, path + "[" + index + "]", "duplicate", "locale 不能重复");
      }
    }
  }

  private void validateCountries(
      List<String> countries, List<ApiFieldErrorResponse> errors, String path) {
    Set<String> values = new HashSet<>();
    for (int index = 0; index < countries.size(); index++) {
      String country = countries.get(index);
      if (!COUNTRY_PATTERN.matcher(country).matches() || !SUPPORTED_COUNTRIES.contains(country)) {
        error(errors, path + "[" + index + "]", "invalid_country", "国家代码必须是首期支持的 ISO alpha-2 代码");
      } else if (!values.add(country)) {
        error(errors, path + "[" + index + "]", "duplicate", "国家代码不能重复");
      }
    }
  }

  private void validateRecognizerConfiguration(
      SensitiveRuleInput rule,
      SensitiveDataType dataType,
      SensitiveRecognizerKind kind,
      List<ApiFieldErrorResponse> errors,
      String path) {
    boolean hasPattern = rule.regexPattern() != null;
    boolean hasDialect = rule.regexDialect() != null;
    boolean hasDictionary = !rule.dictionaryTerms().isEmpty();
    boolean hasValidator = rule.validatorName() != null;

    if (kind == SensitiveRecognizerKind.REGEX) {
      if (!hasPattern || rule.regexPattern().isBlank()) {
        error(errors, path + ".regex_pattern", "required", "regex 规则必须提供非空 pattern");
      } else {
        try {
          Pattern.compile(rule.regexPattern());
        } catch (RuntimeException exception) {
          error(errors, path + ".regex_pattern", "invalid_re2", "pattern 不是有效的 RE2 表达式");
        }
      }
      if (!"re2".equals(rule.regexDialect())) {
        error(errors, path + ".regex_dialect", "unsupported", "自定义正则只接受 re2 方言");
      }
      if (hasDictionary || hasValidator) {
        error(errors, path, "mixed_configuration", "regex 规则不能同时配置词典或校验器");
      }
    } else if (kind == SensitiveRecognizerKind.DICTIONARY) {
      if (!hasDictionary) {
        error(errors, path + ".dictionary_terms", "required", "dictionary 规则至少需要一个词条");
      }
      long totalCharacters = rule.dictionaryTerms().stream().mapToLong(String::length).sum();
      if (totalCharacters > 1_000_000) {
        error(errors, path + ".dictionary_terms", "too_large", "词典总字符数不能超过 1,000,000");
      }
      Set<String> terms = new HashSet<>();
      for (int index = 0; index < rule.dictionaryTerms().size(); index++) {
        if (!terms.add(rule.dictionaryTerms().get(index))) {
          error(
              errors,
              path + ".dictionary_terms[" + index + "]",
              "duplicate",
              "词典词条不能重复");
        }
      }
      if (hasPattern || hasDialect || hasValidator) {
        error(errors, path, "mixed_configuration", "dictionary 规则不能配置正则或校验器");
      }
    } else if (kind == SensitiveRecognizerKind.VALIDATOR) {
      if (!hasValidator || !VALIDATOR_NAMES.contains(rule.validatorName())) {
        error(
            errors,
            path + ".validator_name",
            "unsupported",
            "validator_name 必须引用受控内置校验器");
      }
      if (hasPattern || hasDialect || hasDictionary) {
        error(errors, path, "mixed_configuration", "validator 规则不能配置正则或词典");
      }
    } else if (kind == SensitiveRecognizerKind.PRESIDIO) {
      if (dataType == SensitiveDataType.CUSTOM) {
        error(errors, path + ".data_type", "unsupported", "custom 类型不能使用 presidio 识别器");
      }
      if (hasPattern || hasDialect || hasDictionary || hasValidator) {
        error(errors, path, "mixed_configuration", "presidio 规则不能携带可执行扩展配置");
      }
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
