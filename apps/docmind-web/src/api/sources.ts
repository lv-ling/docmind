import type {
  CompleteSourceUploadRequest,
  CompleteSourceUploadResponse,
  CreateSourceUploadRequest,
  CreateSourceUploadResponse,
  SourceDocumentDetail,
  SourceDocumentPage,
  SourcePreviewAccess,
  SourceVersionId,
  WorkspaceId,
} from '@/contracts';

import { apiRequest, createIdempotencyKey } from './client.js';

export const listSources = (
  workspaceId: WorkspaceId,
  cursor?: string,
): Promise<SourceDocumentPage> => {
  const query = new URLSearchParams({ limit: '50' });
  if (cursor !== undefined) query.set('cursor', cursor);
  return apiRequest(`/api/v1/workspaces/${workspaceId}/sources?${query.toString()}`);
};

export const createSourceUpload = (
  workspaceId: WorkspaceId,
  request: CreateSourceUploadRequest,
): Promise<CreateSourceUploadResponse> =>
  apiRequest(`/api/v1/workspaces/${workspaceId}/sources`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const completeSourceUpload = (
  sourceVersionId: SourceVersionId,
  request: CompleteSourceUploadRequest,
): Promise<CompleteSourceUploadResponse> =>
  apiRequest(`/api/v1/source-versions/${sourceVersionId}/complete`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const getSource = (sourceId: string): Promise<SourceDocumentDetail> =>
  apiRequest(`/api/v1/sources/${sourceId}`);

export const getSourcePreview = (sourceVersionId: SourceVersionId): Promise<SourcePreviewAccess> =>
  apiRequest(`/api/v1/source-versions/${sourceVersionId}/preview`);
