import type {
  CommentId,
  CommentThreadId,
  DocumentInstanceId,
  InstanceVersionId,
  IsoDateTime,
  ProofreadingRunId,
  ProofreadingSuggestionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';

export const COMMENT_THREAD_STATUSES = ['open', 'resolved'] as const;
export type CommentThreadStatus = (typeof COMMENT_THREAD_STATUSES)[number];

export const PROOFREADING_RUN_STATUSES = ['queued', 'running', 'completed', 'failed'] as const;
export type ProofreadingRunStatus = (typeof PROOFREADING_RUN_STATUSES)[number];

export const PROOFREADING_CATEGORIES = [
  'spelling',
  'grammar',
  'punctuation',
  'style',
  'terminology',
] as const;
export type ProofreadingCategory = (typeof PROOFREADING_CATEGORIES)[number];

export const PROOFREADING_SEVERITIES = ['info', 'warning', 'error'] as const;
export type ProofreadingSeverity = (typeof PROOFREADING_SEVERITIES)[number];

export const PROOFREADING_SUGGESTION_STATUSES = ['open', 'accepted', 'dismissed'] as const;
export type ProofreadingSuggestionStatus = (typeof PROOFREADING_SUGGESTION_STATUSES)[number];

/** Resilient text anchor. Quote and surrounding context permit reattachment after later edits. */
export interface ReviewTextAnchor {
  node_id: string;
  start_offset: number;
  end_offset: number;
  quote: string;
  prefix: string;
  suffix: string;
}

export interface CommentThread {
  id: CommentThreadId;
  workspace_id: WorkspaceId;
  instance_id: DocumentInstanceId;
  instance_version_id: InstanceVersionId;
  anchor: ReviewTextAnchor;
  status: CommentThreadStatus;
  created_at: IsoDateTime;
  created_by: UserId;
  resolved_at: IsoDateTime | null;
  resolved_by: UserId | null;
}

export interface Comment {
  id: CommentId;
  thread_id: CommentThreadId;
  body: string;
  created_at: IsoDateTime;
  created_by: UserId;
}

export interface ProofreadingRun {
  id: ProofreadingRunId;
  workspace_id: WorkspaceId;
  instance_id: DocumentInstanceId;
  instance_version_id: InstanceVersionId;
  status: ProofreadingRunStatus;
  engine: string;
  engine_version: string;
  created_at: IsoDateTime;
  completed_at: IsoDateTime | null;
}

export interface ProofreadingSuggestion {
  id: ProofreadingSuggestionId;
  run_id: ProofreadingRunId;
  anchor: ReviewTextAnchor;
  category: ProofreadingCategory;
  severity: ProofreadingSeverity;
  message: string;
  replacement: string | null;
  status: ProofreadingSuggestionStatus;
  resolved_at: IsoDateTime | null;
  resolved_by: UserId | null;
  resolution_reason: string | null;
  applied_instance_version_id: InstanceVersionId | null;
}

export interface CreateCommentThreadRequest {
  anchor: ReviewTextAnchor;
  body: string;
}

export interface ResolveCommentThreadRequest {
  resolved: boolean;
}

export type CreateProofreadingRunRequest = Record<string, never>;

export interface DismissProofreadingSuggestionRequest {
  reason: string | null;
}
