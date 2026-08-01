import type { RequestId } from './ids.js';
import type { JsonObject } from './json.js';
import type { IsoDateTime } from './time.js';

export const API_ERROR_CATEGORIES = [
  'authentication',
  'authorization',
  'validation',
  'resource',
  'conflict',
  'task',
  'dependency',
  'rate_limit',
  'internal',
] as const;

export type ApiErrorCategory = (typeof API_ERROR_CATEGORIES)[number];

export const API_ERROR_CODES = [
  'AUTHENTICATION_REQUIRED',
  'PERMISSION_DENIED',
  'VALIDATION_FAILED',
  'SCHEMA_INVALID',
  'SENSITIVE_DATA_DETECTED',
  'SENSITIVE_SCAN_UNAVAILABLE',
  'UNKNOWN_SENSITIVE_TOKEN',
  'FILE_TYPE_NOT_ALLOWED',
  'FILE_TOO_LARGE',
  'RESOURCE_NOT_FOUND',
  'RESOURCE_CONFLICT',
  'VERSION_CONFLICT',
  'EDIT_LOCK_REQUIRED',
  'EDIT_LOCK_CONFLICT',
  'ANCHOR_STALE',
  'IDEMPOTENCY_CONFLICT',
  'TASK_FAILED',
  'DEPENDENCY_UNAVAILABLE',
  'RATE_LIMITED',
  'INTERNAL_ERROR',
] as const;

export type ApiErrorCode = (typeof API_ERROR_CODES)[number];

export interface ApiFieldError {
  path: string;
  code: string;
  message: string;
}

export interface ApiError {
  code: ApiErrorCode;
  category: ApiErrorCategory;
  message: string;
  details: JsonObject;
  field_errors: ApiFieldError[];
  request_id: RequestId;
  timestamp: IsoDateTime;
}
