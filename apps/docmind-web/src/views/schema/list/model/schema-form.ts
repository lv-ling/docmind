import {
  SENSITIVE_SUPPORTED_COUNTRY_CODES,
  type JsonValue,
  type SchemaFieldInput,
  type SchemaValueType,
  type SensitiveRuleInput,
} from '@/contracts';

export interface EditableSchemaField {
  clientId: string;
  key: string;
  description: string;
  valueType: SchemaValueType;
  required: boolean;
  nullable: boolean;
  sensitivity: SchemaFieldInput['sensitivity'];
  defaultEnabled: boolean;
  defaultValue: string;
  extractionHint: string;
}

export const createEditableSchemaField = (index: number): EditableSchemaField => ({
  clientId: crypto.randomUUID(),
  key: index === 0 ? 'document_title' : '',
  description: index === 0 ? '文档中出现的正式标题' : '',
  valueType: 'string',
  required: false,
  nullable: true,
  sensitivity: 'none',
  defaultEnabled: false,
  defaultValue: '',
  extractionHint: '',
});

export const parseSchemaLiteral = (value: string, type: SchemaValueType): JsonValue => {
  if (type === 'number' || type === 'integer') {
    const number = Number(value);
    if (!Number.isFinite(number)) throw new Error('数字字段的默认值必须是有效数字');
    return number;
  }
  if (type === 'boolean') {
    if (!['true', 'false'].includes(value.toLowerCase())) {
      throw new Error('布尔默认值只能是 true 或 false');
    }
    return value.toLowerCase() === 'true';
  }
  if (type === 'object' || type === 'array') {
    const parsed = JSON.parse(value) as unknown;
    if (parsed === undefined) throw new Error('默认 JSON 无效');
    return parsed as JsonValue;
  }
  return value;
};

export const toSchemaFieldInput = (
  field: EditableSchemaField,
  position: number,
): SchemaFieldInput => ({
  key: field.key.trim(),
  json_path: `$.${field.key.trim()}`,
  description: field.description.trim(),
  value_type: field.valueType,
  array_item_type: field.valueType === 'array' ? 'string' : null,
  required: field.required,
  nullable: field.nullable,
  default: field.defaultEnabled
    ? { kind: 'literal', value: parseSchemaLiteral(field.defaultValue, field.valueType) }
    : { kind: 'none' },
  sensitivity: field.sensitivity,
  constraints: {
    format: null,
    pattern: null,
    enum_values: [],
    min_length: null,
    max_length: null,
    minimum: null,
    maximum: null,
  },
  examples: [],
  extraction_hint: field.extractionHint.trim() || null,
  display: {
    mask: field.sensitivity === 'none' ? 'none' : field.sensitivity === 'high' ? 'full' : 'partial',
    view_role_keys: field.sensitivity === 'none' ? [] : ['owner', 'admin', 'reviewer'],
  },
  metadata: {},
  position,
});

export const createDefaultSensitiveRules = (): SensitiveRuleInput[] => {
  const countries = [...SENSITIVE_SUPPORTED_COUNTRY_CODES];
  const createRule = (
    rule: Omit<
      SensitiveRuleInput,
      'country_codes' | 'locales' | 'confidence_threshold' | 'priority' | 'enabled'
    >,
    priority: number,
  ): SensitiveRuleInput => ({
    ...rule,
    country_codes: countries,
    locales: ['zh-CN', 'en-US', 'ja-JP', 'ko-KR', 'de-DE', 'fr-FR', 'en-GB', 'en-AU', 'nl-NL'],
    confidence_threshold: 0.75,
    priority,
    enabled: true,
  });

  return [
    createRule(
      {
        key: 'international_phone',
        name: '国际电话号码',
        description: '本地格式与 E.164 国际号码',
        data_type: 'phone_number',
        recognizer_kind: 'validator',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: 'e164_phone',
      },
      100,
    ),
    createRule(
      {
        key: 'email_address',
        name: '电子邮箱',
        description: '常见国际化邮箱地址',
        data_type: 'email_address',
        recognizer_kind: 'validator',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: 'email',
      },
      90,
    ),
    createRule(
      {
        key: 'identity_document',
        name: '身份号码',
        description: '九国身份证件号码与校验规则',
        data_type: 'identity_document',
        recognizer_kind: 'presidio',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: null,
      },
      80,
    ),
    createRule(
      {
        key: 'passport_document',
        name: '护照号码',
        description: '九国主流护照号码格式',
        data_type: 'passport',
        recognizer_kind: 'validator',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: 'passport_document',
      },
      70,
    ),
    createRule(
      {
        key: 'bank_account',
        name: '银行卡与银行账户',
        description: '银行卡、IBAN 与本地银行账户标识',
        data_type: 'bank_account',
        recognizer_kind: 'presidio',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: null,
      },
      60,
    ),
    createRule(
      {
        key: 'person_name',
        name: '人员姓名',
        description: '上下文中的自然人姓名',
        data_type: 'person_name',
        recognizer_kind: 'presidio',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: null,
      },
      50,
    ),
  ];
};
