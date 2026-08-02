import type {
  CommentThreadId,
  DiffRunId,
  DocumentInstanceId,
  EventId,
  ExtractionRunId,
  IsoDateTime,
  ProofreadingRunId,
  SourceVersionId,
  TemplateVersionId,
  UserId,
  WorkspaceId,
} from '../common/index.js';

export const DOMAIN_EVENT_TYPES = [
  'source.version.created',
  'extraction.completed',
  'template.generated',
  'template.version.published',
  'instance.lock.acquired',
  'instance.saved',
  'instance.submitted',
  'instance.lock.released',
  'comment.thread.created',
  'proofreading.completed',
  'diff.completed',
] as const;
export type DomainEventType = (typeof DOMAIN_EVENT_TYPES)[number];

export interface DomainEventDataByType {
  'source.version.created': { source_version_id: SourceVersionId };
  'extraction.completed': {
    extraction_run_id: ExtractionRunId;
    status: 'review_required' | 'approved' | 'failed';
  };
  'template.generated': { template_version_id: TemplateVersionId };
  'template.version.published': { template_version_id: TemplateVersionId };
  'instance.lock.acquired': { instance_id: DocumentInstanceId; holder_id: UserId };
  'instance.saved': { instance_id: DocumentInstanceId; revision: number };
  'instance.submitted': { instance_id: DocumentInstanceId; revision: number };
  'instance.lock.released': { instance_id: DocumentInstanceId; holder_id: UserId };
  'comment.thread.created': { comment_thread_id: CommentThreadId };
  'proofreading.completed': { proofreading_run_id: ProofreadingRunId };
  'diff.completed': { diff_run_id: DiffRunId };
}

interface DomainEventEnvelope {
  specversion: '1.0';
  id: EventId;
  source: string;
  subject: string;
  time: IsoDateTime;
  datacontenttype: 'application/json';
  workspace_id: WorkspaceId;
  aggregate_id: string;
  schema_version: string;
  idempotency_key: string | null;
}

/** Event-specific ID/status payloads prevent document text and extracted values crossing the bus. */
export type DomainEvent = {
  [Type in DomainEventType]: DomainEventEnvelope & {
    type: Type;
    data: DomainEventDataByType[Type];
  };
}[DomainEventType];
