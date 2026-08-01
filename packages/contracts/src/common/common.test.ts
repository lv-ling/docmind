import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  API_ERROR_CATEGORIES,
  API_ERROR_CODES,
  DEFAULT_PAGE_LIMIT,
  MAX_PAGE_LIMIT,
  type ApiError,
  type CursorPage,
  type RequestId,
  type SourceDocumentId,
} from '../index.js';

describe('common contracts', () => {
  it('publishes stable pagination limits', () => {
    expect(DEFAULT_PAGE_LIMIT).toBe(20);
    expect(MAX_PAGE_LIMIT).toBe(100);
  });

  it('publishes finite error categories and codes', () => {
    expect(API_ERROR_CATEGORIES).toContain('validation');
    expect(API_ERROR_CODES).toContain('SCHEMA_INVALID');
    expect(API_ERROR_CODES).toContain('SENSITIVE_DATA_DETECTED');
    expect(API_ERROR_CODES).toContain('EDIT_LOCK_CONFLICT');
  });

  it('keeps entity ids distinct and page responses generic', () => {
    expectTypeOf<RequestId>().not.toEqualTypeOf<SourceDocumentId>();
    expectTypeOf<CursorPage<{ name: string }>['items']>().toEqualTypeOf<{ name: string }[]>();
    expectTypeOf<ApiError['details']>().toMatchTypeOf<Record<string, unknown>>();
  });
});
