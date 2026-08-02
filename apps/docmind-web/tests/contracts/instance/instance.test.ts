import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  DOCUMENT_INSTANCE_STATUSES,
  INSTANCE_VERSION_SOURCES,
  type CreateDocumentInstanceRequest,
  type InstanceVersion,
  type SaveDocumentInstanceRequest,
} from '@/contracts';

describe('document-instance contracts', () => {
  it('keeps immutable versions and the editable lifecycle explicit', () => {
    expect(DOCUMENT_INSTANCE_STATUSES).toEqual([
      'draft',
      'editing',
      'saved',
      'submitted',
      'archived',
    ]);
    expect(INSTANCE_VERSION_SOURCES).toContain('submitted');
  });

  it('keeps the immutable document snapshot explicit on save', () => {
    expectTypeOf<CreateDocumentInstanceRequest>().not.toHaveProperty('template_version_id');
    expectTypeOf<SaveDocumentInstanceRequest>().toHaveProperty('document');
    expectTypeOf<InstanceVersion>().toHaveProperty('document');
    expectTypeOf<InstanceVersion>().toHaveProperty('data');
  });
});
