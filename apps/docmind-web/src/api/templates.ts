import type {
  AcceptedTemplateJob,
  CreateTemplateVersionRequest,
  PublishTemplateVersionRequest,
  RollbackTemplateRequest,
  SourceVersionId,
  Template,
  TemplateDetail,
  TemplateId,
  TemplateResource,
  TemplateVersion,
  TemplateVersionId,
  WorkspaceId,
} from '@/contracts';

import { apiRequest, createIdempotencyKey, getAuthenticatedObjectUrl } from './client.js';

export { getAuthenticatedObjectUrl } from './client.js';

export const listTemplates = (workspaceId: WorkspaceId): Promise<Template[]> =>
  apiRequest(`/api/v1/workspaces/${workspaceId}/templates`);

export const createTemplate = (
  sourceVersionId: SourceVersionId,
  name: string,
): Promise<AcceptedTemplateJob> =>
  apiRequest(`/api/v1/source-versions/${sourceVersionId}/templates`, {
    method: 'POST',
    body: { name },
    idempotencyKey: createIdempotencyKey(),
  });

export const getTemplate = (templateId: TemplateId): Promise<TemplateDetail> =>
  apiRequest(`/api/v1/templates/${templateId}`);

export interface NativeEditorSession {
  session_id: string;
  editor_url: string;
  editor_config: Record<string, unknown>;
  expires_at: string;
}

export interface NativeEditorSessionStatus {
  session_id: string;
  template_id: string;
  status: string;
  expires_at: string;
  last_callback_status: number | null;
  callback_count: number;
  saved_sha256: string | null;
  saved_size_bytes: number | null;
  saved_at: string | null;
}

export const createNativeEditorSession = (templateId: TemplateId): Promise<NativeEditorSession> =>
  apiRequest(`/api/v1/templates/${templateId}/editor-sessions`, { method: 'POST' });

export const getNativeEditorSessionStatus = (
  sessionId: string,
): Promise<NativeEditorSessionStatus> =>
  apiRequest(`/api/v1/template-editor-sessions/${sessionId}`);

export const createTemplateVersion = (
  templateId: TemplateId,
  request: CreateTemplateVersionRequest,
): Promise<TemplateVersion> =>
  apiRequest(`/api/v1/templates/${templateId}/versions`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const publishTemplateVersion = (
  templateId: TemplateId,
  versionId: TemplateVersionId,
  request: PublishTemplateVersionRequest,
): Promise<TemplateVersion> =>
  apiRequest(`/api/v1/templates/${templateId}/versions/${versionId}/publish`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const rollbackTemplate = (
  templateId: TemplateId,
  request: RollbackTemplateRequest,
): Promise<TemplateVersion> =>
  apiRequest(`/api/v1/templates/${templateId}/rollback`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const hydrateTemplateHtml = async (
  html: string,
  resources: TemplateResource[],
): Promise<{ html: string; objectUrls: string[] }> => {
  let hydrated = html;
  const objectUrls: string[] = [];
  for (const resource of resources) {
    const objectUrl = await getAuthenticatedObjectUrl(resource.download_url);
    objectUrls.push(objectUrl);
    hydrated = hydrated.replaceAll(resource.download_url, objectUrl);
  }
  return { html: hydrated, objectUrls };
};
