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
} from '@docmind/contracts';

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
