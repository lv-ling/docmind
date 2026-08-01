import type {
  AuditMetadata,
  ConversionWarningId,
  IsoDateTime,
  JobId,
  JsonObject,
  ParsedContentId,
  SourceDocumentId,
  SourceVersionId,
  TemplateId,
  TemplateResourceId,
  TemplateVersionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';

export const TEMPLATE_VERSION_STATUSES = [
  'generated',
  'checking',
  'published',
  'superseded',
] as const;
export type TemplateVersionStatus = (typeof TEMPLATE_VERSION_STATUSES)[number];

export const TEMPLATE_CONVERSION_STATUSES = [
  'queued',
  'running',
  'ready',
  'retrying',
  'failed',
] as const;
export type TemplateConversionStatus = (typeof TEMPLATE_CONVERSION_STATUSES)[number];

export const TEMPLATE_RESOURCE_KINDS = ['image', 'stylesheet', 'font', 'attachment'] as const;
export type TemplateResourceKind = (typeof TEMPLATE_RESOURCE_KINDS)[number];

export const CONVERSION_WARNING_SEVERITIES = ['info', 'warning', 'error'] as const;
export type ConversionWarningSeverity = (typeof CONVERSION_WARNING_SEVERITIES)[number];

export interface Template extends AuditMetadata {
  id: TemplateId;
  workspace_id: WorkspaceId;
  source_document_id: SourceDocumentId;
  source_version_id: SourceVersionId;
  conversion_job_id: JobId;
  conversion_status: TemplateConversionStatus;
  failure_code: string | null;
  name: string;
  current_version_id: TemplateVersionId | null;
}

export interface ParsedContentReference {
  id: ParsedContentId;
  source_version_id: SourceVersionId;
  parser_name: string;
  parser_version: string;
  document_model: JsonObject;
}

export interface TemplateResource {
  id: TemplateResourceId;
  kind: TemplateResourceKind;
  content_type: string;
  byte_size: number;
  sha256: string;
  /** Short-lived application URL; never an object-storage key or bucket path. */
  download_url: string;
}

export interface ConversionWarning {
  id: ConversionWarningId;
  severity: ConversionWarningSeverity;
  code: string;
  message: string;
  source_node_id: string | null;
  page_number: number | null;
  fallback: string | null;
  blocking: boolean;
}

export interface SafeHtmlDocument {
  html: string;
  css: string;
  sanitization_policy_version: string;
}

export interface TemplateVersion {
  id: TemplateVersionId;
  template_id: TemplateId;
  workspace_id: WorkspaceId;
  source_version_id: SourceVersionId;
  parsed_content_id: ParsedContentId;
  version_number: number;
  status: TemplateVersionStatus;
  document: SafeHtmlDocument;
  document_model: JsonObject;
  resources: TemplateResource[];
  warnings: ConversionWarning[];
  change_summary: string;
  diff: JsonObject;
  created_at: IsoDateTime;
  created_by: UserId;
  published_at: IsoDateTime | null;
}

export interface CreateTemplateRequest {
  name: string;
}

export interface CreateTemplateVersionRequest {
  base_version_id: TemplateVersionId;
  document_model: JsonObject;
  change_summary: string;
}

export interface AcceptedTemplateJob {
  job_id: JobId;
  template_id: TemplateId;
  request_id: import('../common/index.js').RequestId;
}

export interface TemplateDetail {
  template: Template;
  current_version: TemplateVersion | null;
  versions: TemplateVersion[];
}

export interface PublishTemplateVersionRequest {
  note: string | null;
}

export interface RollbackTemplateRequest {
  target_version_id: TemplateVersionId;
  change_summary: string;
}

/** Trusted converter output; never accepted directly from a Web client. */
export interface GeneratedTemplateVersionInput {
  source_version_id: SourceVersionId;
  parsed_content_id: ParsedContentId;
  document: SafeHtmlDocument;
  resources: TemplateResource[];
  warnings: ConversionWarning[];
}
