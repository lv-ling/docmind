import type {
  CreateSchemaRequest,
  CreateSensitiveRuleTemplateRequest,
  ExtractionSchema,
  SchemaTemplate,
  SensitiveRuleTemplate,
  WorkspaceId,
} from '@/contracts';

import { apiRequest, createIdempotencyKey } from './client.js';

export interface ExtractionSchemaDetail {
  schema: ExtractionSchema;
  current_version: import('@/contracts').SchemaVersion | null;
  versions: import('@/contracts').SchemaVersion[];
}

export interface SensitiveRuleTemplateDetail {
  template: SensitiveRuleTemplate;
  current_version: import('@/contracts').SensitiveRuleTemplateVersion | null;
  versions: import('@/contracts').SensitiveRuleTemplateVersion[];
}

export const listSchemas = (workspaceId: WorkspaceId): Promise<ExtractionSchema[]> =>
  apiRequest(`/api/v1/workspaces/${workspaceId}/schemas`);

export const createSchema = (
  workspaceId: WorkspaceId,
  request: CreateSchemaRequest,
): Promise<ExtractionSchemaDetail> =>
  apiRequest(`/api/v1/workspaces/${workspaceId}/schemas`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const listSchemaTemplates = (workspaceId: WorkspaceId): Promise<SchemaTemplate[]> =>
  apiRequest(`/api/v1/workspaces/${workspaceId}/schema-templates`);

export const listSensitiveRuleTemplates = (
  workspaceId: WorkspaceId,
): Promise<SensitiveRuleTemplate[]> =>
  apiRequest(`/api/v1/workspaces/${workspaceId}/sensitive-rule-templates`);

export const createSensitiveRuleTemplate = (
  workspaceId: WorkspaceId,
  request: CreateSensitiveRuleTemplateRequest,
): Promise<SensitiveRuleTemplateDetail> =>
  apiRequest(`/api/v1/workspaces/${workspaceId}/sensitive-rule-templates`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });
