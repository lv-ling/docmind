import type { SchemaFieldId, SchemaVersionId } from '@/contracts';
import { describe, expect, it } from 'vitest';

import {
  InvalidControlledDocumentError,
  assertValidControlledDocument,
  createEmptyDocument,
  validateControlledDocument,
  type DocumentNodeId,
  type TemplateBindingId,
} from '@/editor';

const nodeId = (value: string): DocumentNodeId => value as DocumentNodeId;

describe('controlled document validation', () => {
  it('rejects duplicate node IDs', () => {
    const document = createEmptyDocument({
      root_id: nodeId('root'),
      title: 'Test',
      language: 'en',
    });
    document.blocks = [
      {
        id: nodeId('duplicate'),
        type: 'paragraph',
        source: null,
        attributes: {},
        content: [],
        style: {},
      },
      {
        id: nodeId('duplicate'),
        type: 'page_break',
        source: null,
        attributes: {},
      },
    ];

    expect(validateControlledDocument(document).issues).toEqual(
      expect.arrayContaining([expect.objectContaining({ code: 'NODE_ID_DUPLICATE' })]),
    );
    expect(() => assertValidControlledDocument(document)).toThrow(InvalidControlledDocumentError);
  });

  it('rejects placeholder bindings from another schema version', () => {
    const document = createEmptyDocument({
      root_id: nodeId('root'),
      title: 'Test',
      language: 'en',
      template_schema_version_id: 'schema-v1' as SchemaVersionId,
    });
    document.blocks = [
      {
        id: nodeId('paragraph'),
        type: 'paragraph',
        source: null,
        attributes: {},
        style: {},
        content: [
          {
            id: nodeId('placeholder'),
            type: 'template_placeholder',
            source: null,
            attributes: {},
            label: 'Name',
            style: {},
            binding: {
              id: 'binding' as TemplateBindingId,
              schema_version_id: 'schema-v2' as SchemaVersionId,
              schema_field_id: 'field' as SchemaFieldId,
              json_path: '$.name',
              value_type: 'string',
              path_scope: 'absolute',
              value_policy: 'plain_text',
              missing_behavior: 'error',
              format: null,
            },
          },
        ],
      },
    ];

    expect(validateControlledDocument(document).issues).toEqual(
      expect.arrayContaining([expect.objectContaining({ code: 'SCHEMA_VERSION_MISMATCH' })]),
    );
  });

  it('rejects unsupported model versions from untrusted JSON', () => {
    expect(validateControlledDocument({ model_version: '999' }).issues).toEqual(
      expect.arrayContaining([expect.objectContaining({ code: 'MODEL_VERSION_UNSUPPORTED' })]),
    );
  });

  it('rejects unsafe page dimensions before CSS serialization', () => {
    const document = createEmptyDocument({
      root_id: nodeId('root'),
      title: 'Test',
      language: 'en',
    });
    document.page_layout.width.value = Number.POSITIVE_INFINITY;

    expect(validateControlledDocument(document).issues).toEqual(
      expect.arrayContaining([expect.objectContaining({ code: 'LENGTH_VALUE_INVALID' })]),
    );
  });
});
