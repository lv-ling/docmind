import type { IsoDateTime, UserId, WorkspaceId, WorkspaceMemberId } from '../common/index.js';

export const USER_STATUSES = ['active', 'disabled'] as const;
export type UserStatus = (typeof USER_STATUSES)[number];

export const WORKSPACE_ROLES = ['owner', 'admin', 'editor', 'reviewer', 'viewer'] as const;
export type WorkspaceRole = (typeof WORKSPACE_ROLES)[number];

export const MEMBER_STATUSES = ['active', 'suspended'] as const;
export type MemberStatus = (typeof MEMBER_STATUSES)[number];

export interface UserSummary {
  id: UserId;
  email: string;
  display_name: string;
  status: UserStatus;
}

export interface WorkspaceSummary {
  id: WorkspaceId;
  name: string;
  slug: string;
  role: WorkspaceRole;
  created_at: IsoDateTime;
}

export interface WorkspaceMember {
  id: WorkspaceMemberId;
  workspace_id: WorkspaceId;
  user: UserSummary;
  role: WorkspaceRole;
  status: MemberStatus;
  created_at: IsoDateTime;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  token_type: 'Bearer';
  expires_in: number;
  user: UserSummary;
}

export interface CreateWorkspaceRequest {
  name: string;
  slug: string;
}
