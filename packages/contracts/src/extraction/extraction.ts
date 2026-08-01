import type {
  ExtractionCandidateId,
  ExtractionEvidenceId,
  ExtractionFieldResultId,
  ExtractionRunId,
  IsoDateTime,
  JobId,
  JsonObject,
  JsonValue,
  RequestId,
  SchemaVersionId,
  SensitiveRuleTemplateVersionId,
  SourceVersionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';

export const EXTRACTION_RUN_STATUSES = [
  'queued',
  'running',
  'review_required',
  'approved',
  'failed',
  'retrying',
] as const;
export type ExtractionRunStatus = (typeof EXTRACTION_RUN_STATUSES)[number];

export const EXTRACTION_VALUE_SOURCES = ['extracted', 'default', 'manual', 'null'] as const;
export type ExtractionValueSource = (typeof EXTRACTION_VALUE_SOURCES)[number];

export const EXTRACTION_MISSING_REASONS = [
  'not_found',
  'insufficient_evidence',
  'ambiguous',
  'invalid_model_output',
  'sensitive_token_missing',
] as const;
export type ExtractionMissingReason = (typeof EXTRACTION_MISSING_REASONS)[number];

export const FIELD_REVIEW_STATUSES = ['pending', 'accepted', 'modified', 'rejected'] as const;
export type FieldReviewStatus = (typeof FIELD_REVIEW_STATUSES)[number];

export interface ExtractionEvidence {
  id: ExtractionEvidenceId;
  source_version_id: SourceVersionId;
  page_number: number | null;
  node_id: string;
  text: string;
  start_offset: number | null;
  end_offset: number | null;
}

/** Permission-filtered evidence returned to Web clients. It never carries hidden plaintext. */
export interface ExtractionEvidenceView {
  page_number: number | null;
  node_id: string;
  display_text: string;
  is_masked: boolean;
  start_offset: number | null;
  end_offset: number | null;
}

export type ExtractionDisplayValue =
  | { access: 'visible'; value: JsonValue }
  | { access: 'masked'; value: null; masked_preview: string };

export interface ExtractionCandidate {
  id: ExtractionCandidateId;
  value: JsonValue;
  confidence: number;
  evidence: ExtractionEvidence[];
}

export interface ExtractionCandidateView {
  display_value: ExtractionDisplayValue;
  confidence: number;
  evidence: ExtractionEvidenceView[];
}

export interface ExtractionFieldResult {
  id: ExtractionFieldResultId;
  json_path: string;
  value: JsonValue;
  value_source: ExtractionValueSource;
  missing_reason: ExtractionMissingReason | null;
  confidence: number | null;
  evidence: ExtractionEvidence[];
  candidates: ExtractionCandidate[];
  needs_review: boolean;
  review_status: FieldReviewStatus;
  reviewed_value: JsonValue | null;
  reviewed_by: UserId | null;
  reviewed_at: IsoDateTime | null;
}

/** HTTP-safe field result after field-level authorization and masking. */
export interface ExtractionFieldResultView {
  id: ExtractionFieldResultId;
  json_path: string;
  display_value: ExtractionDisplayValue;
  value_source: ExtractionValueSource;
  missing_reason: ExtractionMissingReason | null;
  confidence: number | null;
  evidence: ExtractionEvidenceView[];
  candidates: ExtractionCandidateView[];
  needs_review: boolean;
  review_status: FieldReviewStatus;
}

export interface ExtractionModelMetadata {
  provider: string;
  model: string;
  prompt_version: string;
  input_tokens: number | null;
  output_tokens: number | null;
}

export interface ExtractionResult {
  data: JsonObject;
  fields: ExtractionFieldResult[];
  model: ExtractionModelMetadata;
  validation_errors: string[];
}

/** Permission-filtered extraction response. `data` contains display values only. */
export interface ExtractionResultView {
  data: JsonObject;
  contains_masked_values: boolean;
  fields: ExtractionFieldResultView[];
  model: ExtractionModelMetadata;
  validation_errors: string[];
}

export interface ExtractionRun {
  id: ExtractionRunId;
  job_id: JobId;
  workspace_id: WorkspaceId;
  source_version_id: SourceVersionId;
  schema_version_id: SchemaVersionId;
  sensitive_rule_template_version_id: SensitiveRuleTemplateVersionId | null;
  status: ExtractionRunStatus;
  result: ExtractionResult | null;
  failure_code: string | null;
  created_at: IsoDateTime;
  completed_at: IsoDateTime | null;
}

/** HTTP-safe extraction status and result after object- and field-level authorization. */
export interface ExtractionRunView {
  id: ExtractionRunId;
  job_id: JobId;
  workspace_id: WorkspaceId;
  source_version_id: SourceVersionId;
  schema_version_id: SchemaVersionId;
  sensitive_rule_template_version_id: SensitiveRuleTemplateVersionId | null;
  status: ExtractionRunStatus;
  result: ExtractionResultView | null;
  failure_code: string | null;
  created_at: IsoDateTime;
  completed_at: IsoDateTime | null;
}

/** Correlation IDs returned when an extraction is accepted for asynchronous execution. */
export interface AcceptedExtractionJob {
  job_id: JobId;
  extraction_id: ExtractionRunId;
  request_id: RequestId;
}

export interface CreateExtractionRequest {
  schema_version_id: SchemaVersionId;
  sensitive_rule_template_version_id: SensitiveRuleTemplateVersionId | null;
}

export interface ReviewExtractionFieldRequest {
  action: 'accept' | 'modify' | 'reject';
  value: JsonValue | null;
  reason: string | null;
}

export interface ApproveExtractionRequest {
  note: string | null;
}
