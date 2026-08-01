import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  MAX_SOURCE_FILE_SIZE_BYTES,
  SOURCE_FILE_TYPES,
  SOURCE_MIME_TYPES,
  SOURCE_VERSION_STATUSES,
  type CompleteSourceUploadRequest,
  type CreateSourceUploadRequest,
  type SourceDocument,
  type SourceDocumentDetail,
  type SourcePreviewAccess,
  type SourceVersion,
  type UploadSession,
} from '../index.js';

describe('source contracts', () => {
  it('restricts source files to DOC/DOCX/PDF and 10 MiB', () => {
    expect(SOURCE_FILE_TYPES).toEqual(['doc', 'docx', 'pdf']);
    expect(SOURCE_MIME_TYPES).toHaveLength(3);
    expect(MAX_SOURCE_FILE_SIZE_BYTES).toBe(10_485_760);
  });

  it('covers the immutable source processing lifecycle', () => {
    expect(SOURCE_VERSION_STATUSES).toEqual([
      'uploading',
      'uploaded',
      'processing',
      'ready',
      'failed',
    ]);
  });

  it('requires upload integrity metadata and a short-lived upload URL', () => {
    expectTypeOf<CompleteSourceUploadRequest>().toHaveProperty('sha256');
    expectTypeOf<CompleteSourceUploadRequest>().toHaveProperty('object_etag');
    expectTypeOf<UploadSession['upload_method']>().toEqualTypeOf<'PUT'>();
    expectTypeOf<SourceDocument['current_version_id']>().toBeNullable();
    expectTypeOf<SourceVersion['file']>().toBeNullable();
    expectTypeOf<CreateSourceUploadRequest['declared_mime_type']>().toBeString();
  });

  it('exposes immutable version history and same-origin read-only access', () => {
    expectTypeOf<SourceDocumentDetail['versions']>().toBeArray();
    expectTypeOf<SourcePreviewAccess['view_url']>().toBeNullable();
    expectTypeOf<SourcePreviewAccess['original_content_url']>().toBeString();
  });
});
