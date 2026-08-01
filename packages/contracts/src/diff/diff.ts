import type {
  DiffChangeId,
  DiffRunId,
  InstanceVersionId,
  IsoDateTime,
  SourceVersionId,
  TemplateVersionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';

export const DIFF_RUN_STATUSES = ['queued', 'parsing', 'comparing', 'completed', 'failed'] as const;
export type DiffRunStatus = (typeof DIFF_RUN_STATUSES)[number];

export const DIFF_DOCUMENT_KINDS = [
  'source_version',
  'template_version',
  'instance_version',
] as const;
export type DiffDocumentKind = (typeof DIFF_DOCUMENT_KINDS)[number];

export type StoredDocumentReference =
  | { kind: 'source_version'; source_version_id: SourceVersionId }
  | { kind: 'template_version'; template_version_id: TemplateVersionId }
  | { kind: 'instance_version'; instance_version_id: InstanceVersionId };

export const DIFF_CHANGE_KINDS = ['insert', 'delete', 'replace', 'format', 'move'] as const;
export type DiffChangeKind = (typeof DIFF_CHANGE_KINDS)[number];

export interface DiffAnchor {
  node_id: string;
  start_offset: number | null;
  end_offset: number | null;
  json_path: string | null;
}

export interface DiffChange {
  id: DiffChangeId;
  kind: DiffChangeKind;
  before: DiffAnchor | null;
  after: DiffAnchor | null;
  before_text: string | null;
  after_text: string | null;
}

/** A persisted, canonical comparison between immutable stored versions. */
export interface DiffRun {
  id: DiffRunId;
  workspace_id: WorkspaceId;
  baseline: StoredDocumentReference;
  target: StoredDocumentReference;
  status: DiffRunStatus;
  algorithm_version: string;
  changes: DiffChange[] | null;
  failure_code: string | null;
  created_at: IsoDateTime;
  completed_at: IsoDateTime | null;
  created_by: UserId;
}

export interface CreateDiffRunRequest {
  baseline: StoredDocumentReference;
  target: StoredDocumentReference;
}
