import type {
  AuditMetadata,
  IsoDateTime,
  SensitiveRuleId,
  SensitiveRuleTemplateId,
  SensitiveRuleTemplateVersionId,
  SensitiveTokenId,
  SourceVersionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';

export const SENSITIVE_DATA_TYPES = [
  'china_national_id',
  'identity_document',
  'passport',
  'phone_number',
  'email_address',
  'credit_card',
  'bank_account',
  'ip_address',
  'person_name',
  'location',
  'custom',
] as const;
export type SensitiveDataType = (typeof SENSITIVE_DATA_TYPES)[number];

/** ISO 3166-1 alpha-2 countries covered by the built-in v0.1 recognizer suite. */
export const SENSITIVE_SUPPORTED_COUNTRY_CODES = [
  'CN',
  'US',
  'JP',
  'KR',
  'DE',
  'FR',
  'GB',
  'AU',
  'NL',
] as const;
export type SensitiveSupportedCountryCode = (typeof SENSITIVE_SUPPORTED_COUNTRY_CODES)[number];

export const SENSITIVE_RECOGNIZER_KINDS = ['presidio', 'regex', 'dictionary', 'validator'] as const;
export type SensitiveRecognizerKind = (typeof SENSITIVE_RECOGNIZER_KINDS)[number];

export const SENSITIVE_REGEX_DIALECTS = ['re2'] as const;
export type SensitiveRegexDialect = (typeof SENSITIVE_REGEX_DIALECTS)[number];

/** Names resolved through an application allowlist; never dynamically loaded from user input. */
export const SENSITIVE_VALIDATOR_NAMES = [
  'cn_resident_identity',
  'e164_phone',
  'email',
  'luhn',
  'iban',
  'ip_address',
  'passport_document',
] as const;
export type SensitiveValidatorName = (typeof SENSITIVE_VALIDATOR_NAMES)[number];

export const SENSITIVE_RULE_TEMPLATE_VERSION_STATUSES = [
  'draft',
  'published',
  'superseded',
] as const;
export type SensitiveRuleTemplateVersionStatus =
  (typeof SENSITIVE_RULE_TEMPLATE_VERSION_STATUSES)[number];

export const SENSITIVE_TOKEN_PATTERN_SOURCE = '^\\[\\[SENSITIVE:[A-Z][A-Z0-9_]*:[0-9]{2,}\\]\\]$';

export interface SensitiveRuleDefinition {
  id: SensitiveRuleId;
  key: string;
  name: string;
  description: string;
  data_type: SensitiveDataType;
  recognizer_kind: SensitiveRecognizerKind;
  locales: string[];
  country_codes: string[];
  regex_pattern: string | null;
  regex_dialect: SensitiveRegexDialect | null;
  dictionary_terms: string[];
  validator_name: SensitiveValidatorName | null;
  confidence_threshold: number;
  priority: number;
  enabled: boolean;
}

/** Client-supplied rule definition. The service assigns the immutable rule ID. */
export interface SensitiveRuleInput {
  key: string;
  name: string;
  description: string;
  data_type: SensitiveDataType;
  recognizer_kind: SensitiveRecognizerKind;
  locales: string[];
  country_codes: string[];
  regex_pattern: string | null;
  regex_dialect: SensitiveRegexDialect | null;
  dictionary_terms: string[];
  validator_name: SensitiveValidatorName | null;
  confidence_threshold: number;
  priority: number;
  enabled: boolean;
}

export interface SensitiveRuleTemplate extends AuditMetadata {
  id: SensitiveRuleTemplateId;
  workspace_id: WorkspaceId;
  name: string;
  description: string;
  current_version_id: SensitiveRuleTemplateVersionId | null;
}

export interface SensitiveRuleTemplateVersion {
  id: SensitiveRuleTemplateVersionId;
  template_id: SensitiveRuleTemplateId;
  workspace_id: WorkspaceId;
  version_number: number;
  status: SensitiveRuleTemplateVersionStatus;
  rules: SensitiveRuleDefinition[];
  change_summary: string;
  created_at: IsoDateTime;
  created_by: UserId;
  published_at: IsoDateTime | null;
}

export interface SensitiveTextSpan {
  node_id: string;
  start_offset: number;
  end_offset: number;
}

/** Safe token metadata. Original values and encrypted mappings never cross this boundary. */
export interface SensitiveTokenReference {
  id: SensitiveTokenId;
  source_version_id: SourceVersionId;
  token: string;
  data_type: SensitiveDataType;
  masked_preview: string;
  occurrences: SensitiveTextSpan[];
}

export interface CreateSensitiveRuleTemplateRequest {
  name: string;
  description: string;
  rules: SensitiveRuleInput[];
}

export interface CreateSensitiveRuleTemplateVersionRequest {
  rules: SensitiveRuleInput[];
  change_summary: string;
}
