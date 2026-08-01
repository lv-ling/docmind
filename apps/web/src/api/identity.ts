import type {
  LoginRequest,
  LoginResponse,
  UserSummary,
  WorkspaceSummary,
} from '@docmind/contracts';

import { apiRequest } from './client.js';

export const login = (request: LoginRequest): Promise<LoginResponse> =>
  apiRequest('/api/v1/auth/login', { method: 'POST', body: request });

export const getCurrentUser = (): Promise<UserSummary> => apiRequest('/api/v1/me');

export const listWorkspaces = (): Promise<WorkspaceSummary[]> => apiRequest('/api/v1/workspaces');
