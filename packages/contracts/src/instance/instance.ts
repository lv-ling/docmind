import type {
  AuditMetadata,
  DocumentInstanceId,
  EditLockId,
  InstanceVersionId,
  IsoDateTime,
  JsonObject,
  TemplateVersionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';
import type { SafeHtmlDocument } from '../template/index.js';

export const DOCUMENT_INSTANCE_STATUSES = [
  'draft',
  'editing',
  'saved',
  'submitted',
  'archived',
] as const;
export type DocumentInstanceStatus = (typeof DOCUMENT_INSTANCE_STATUSES)[number];

export const INSTANCE_VERSION_SOURCES = ['created', 'saved', 'submitted', 'restored'] as const;
export type InstanceVersionSource = (typeof INSTANCE_VERSION_SOURCES)[number];

export interface DocumentInstance extends AuditMetadata {
  id: DocumentInstanceId;
  workspace_id: WorkspaceId;
  template_version_id: TemplateVersionId;
  name: string;
  status: DocumentInstanceStatus;
  current_version_id: InstanceVersionId | null;
  submitted_at: IsoDateTime | null;
  submitted_by: UserId | null;
}

/** Immutable content and data snapshot. The revision is required for optimistic writes. */
export interface InstanceVersion {
  id: InstanceVersionId;
  instance_id: DocumentInstanceId;
  workspace_id: WorkspaceId;
  version_number: number;
  revision: number;
  source: InstanceVersionSource;
  document: SafeHtmlDocument;
  data: JsonObject;
  change_summary: string;
  created_at: IsoDateTime;
  created_by: UserId;
}

export interface EditLock {
  id: EditLockId;
  instance_id: DocumentInstanceId;
  workspace_id: WorkspaceId;
  holder_id: UserId;
  acquired_at: IsoDateTime;
  renewed_at: IsoDateTime;
  expires_at: IsoDateTime;
}

export interface CreateDocumentInstanceRequest {
  name: string;
  initial_data: JsonObject;
}

export interface SaveDocumentInstanceRequest {
  document: SafeHtmlDocument;
  data: JsonObject;
  change_summary: string;
}

export interface SubmitDocumentInstanceRequest {
  note: string | null;
}
