import type {
  AuditMetadata,
  CursorPage,
  IsoDateTime,
  SourceDocumentId,
  SourcePreviewId,
  SourceVersionId,
  UploadSessionId,
  WorkspaceId,
} from '../common/index.js';

export const MAX_SOURCE_FILE_SIZE_BYTES = 10 * 1024 * 1024;

export const SOURCE_FILE_TYPES = ['doc', 'docx', 'pdf'] as const;
export type SourceFileType = (typeof SOURCE_FILE_TYPES)[number];

export const SOURCE_MIME_TYPES = [
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/pdf',
] as const;
export type SourceMimeType = (typeof SOURCE_MIME_TYPES)[number];

export const SOURCE_VERSION_STATUSES = [
  'uploading',
  'uploaded',
  'processing',
  'ready',
  'failed',
] as const;
export type SourceVersionStatus = (typeof SOURCE_VERSION_STATUSES)[number];

export const UPLOAD_SESSION_STATUSES = [
  'pending',
  'uploading',
  'completed',
  'expired',
  'aborted',
] as const;
export type UploadSessionStatus = (typeof UPLOAD_SESSION_STATUSES)[number];

export const SOURCE_PREVIEW_STATUSES = ['queued', 'processing', 'ready', 'failed'] as const;
export type SourcePreviewStatus = (typeof SOURCE_PREVIEW_STATUSES)[number];

export const SOURCE_PREVIEW_FORMATS = ['pdf', 'page_images'] as const;
export type SourcePreviewFormat = (typeof SOURCE_PREVIEW_FORMATS)[number];

export interface SourceDocument extends AuditMetadata {
  id: SourceDocumentId;
  workspace_id: WorkspaceId;
  name: string;
  current_version_id: SourceVersionId | null;
}

export interface SourceFileMetadata {
  original_file_name: string;
  file_type: SourceFileType;
  mime_type: SourceMimeType;
  size_bytes: number;
  sha256: string;
}

export interface SourceVersion {
  id: SourceVersionId;
  source_document_id: SourceDocumentId;
  workspace_id: WorkspaceId;
  version_number: number;
  status: SourceVersionStatus;
  original_file_name: string;
  file_type: SourceFileType;
  /** Browser-provided value retained for audit only; never used as authoritative MIME data. */
  declared_mime_type: string;
  expected_size_bytes: number;
  /** Populated only after the object has passed server-side file validation. */
  file: SourceFileMetadata | null;
  failure_code: string | null;
  created_at: IsoDateTime;
}

export interface SourcePreview {
  id: SourcePreviewId;
  source_version_id: SourceVersionId;
  status: SourcePreviewStatus;
  format: SourcePreviewFormat;
  page_count: number | null;
  failure_code: string | null;
  created_at: IsoDateTime;
  completed_at: IsoDateTime | null;
}

export interface CreateSourceUploadRequest {
  document_name: string;
  original_file_name: string;
  /** Browser-provided hint only. The service must not trust it for file acceptance. */
  declared_mime_type: string;
  size_bytes: number;
}

export type CreateSourceVersionUploadRequest = Omit<CreateSourceUploadRequest, 'document_name'>;

export interface UploadSession {
  id: UploadSessionId;
  source_document_id: SourceDocumentId;
  source_version_id: SourceVersionId;
  status: UploadSessionStatus;
  /** Null after the session becomes terminal; terminal sessions can never mint another PUT URL. */
  upload_url: string | null;
  upload_method: 'PUT';
  required_headers: Record<string, string>;
  max_size_bytes: number;
  expires_at: IsoDateTime;
  created_at: IsoDateTime;
}

export interface CreateSourceUploadResponse {
  source: SourceDocument;
  version: SourceVersion;
  upload: UploadSession;
}

export interface CompleteSourceUploadRequest {
  size_bytes: number;
  detected_mime_type: SourceMimeType;
  sha256: string;
  object_etag: string;
}

export interface CompleteSourceUploadResponse {
  source: SourceDocument;
  version: SourceVersion;
}

export interface SourceDocumentDetail {
  source: SourceDocument;
  versions: SourceVersion[];
}

export type SourceDocumentPage = CursorPage<SourceDocument>;

export interface SourcePreviewAccess {
  preview: SourcePreview;
  /** Same-origin, authenticated URL. Null while a derived preview is unavailable. */
  view_url: string | null;
  /** Same-origin, authenticated URL for the immutable original object. */
  original_content_url: string;
}
