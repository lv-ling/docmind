import type { UserId } from './ids.js';
import type { IsoDateTime } from './time.js';

export interface AuditMetadata {
  created_at: IsoDateTime;
  created_by: UserId;
  updated_at: IsoDateTime;
  updated_by: UserId;
}

export interface SoftDeleteMetadata {
  deleted_at: IsoDateTime | null;
  deleted_by: UserId | null;
}
