import type { SchemaFieldId, SchemaVersionId } from '@/contracts';
import { describe, expect, it } from 'vitest';

import {
  HTML_SANITIZATION_POLICY_VERSION,
  UnsafeControlledHtmlError,
  applyControlledHtmlPolicy,
  createEmptyDocument,
  deserializeControlledDocument,
  deserializeSafeHtmlDocument,
  serializeControlledDocument,
  type DocumentNodeId,
  type TemplateBindingId,
} from '../index.js';

const nodeId = (value: string): DocumentNodeId => value as DocumentNodeId;
const bindingId = (value: string): TemplateBindingId => value as TemplateBindingId;
const schemaVersionId = (value: string): SchemaVersionId => value as SchemaVersionId;
const schemaFieldId = (value: string): SchemaFieldId => value as SchemaFieldId;

describe('controlled HTML', () => {
  it('round-trips a versioned document without executing document text', () => {
    const document = createEmptyDocument({
      root_id: nodeId('root'),
      title: 'Safe contract',
      language: 'zh-CN',
      template_schema_version_id: schemaVersionId('schema-v1'),
    });
    document.blocks = [
      {
        id: nodeId('paragraph'),
        type: 'paragraph',
        source: { source_node_id: 'source-p1', page_number: 1 },
        attributes: {},
        style: { alignment: 'justify' },
        content: [
          {
            id: nodeId('text'),
            type: 'text',
            source: null,
            attributes: {},
            text: '<script>alert(1)</script> & agreement',
            style: { font_size: { value: 12, unit: 'pt' } },
          },
          {
            id: nodeId('placeholder'),
            type: 'template_placeholder',
            source: null,
            attributes: {},
            binding: {
              id: bindingId('binding-party'),
              schema_version_id: schemaVersionId('schema-v1'),
              schema_field_id: schemaFieldId('field-party'),
              json_path: '$.party_name',
              value_type: 'string',
              path_scope: 'absolute',
              value_policy: 'plain_text',
              missing_behavior: 'show_label',
              format: null,
            },
            label: 'Party </template>',
            style: {},
          },
        ],
      },
    ];

    const serialized = serializeControlledDocument(document);

    expect(serialized.sanitization_policy_version).toBe(HTML_SANITIZATION_POLICY_VERSION);
    expect(serialized.html).not.toContain('<script>');
    expect(serialized.html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;');
    expect(deserializeControlledDocument(serialized.html)).toEqual(document);
    expect(deserializeSafeHtmlDocument(serialized)).toEqual(document);
  });

  it('removes executable tags, event handlers, external URLs, and unsafe CSS', () => {
    const result = applyControlledHtmlPolicy(
      '<p onclick="steal()" style="color:#000;background-image:url(javascript:bad)">safe<script>bad</script><img src="https://evil.test/x" onerror="bad()"></p>',
    );

    expect(result.valid).toBe(false);
    expect(result.html).toBe('<p style="color:#000;">safe<img></p>');
    expect(result.html).not.toMatch(/script|onclick|onerror|src=/u);
  });

  it('strictly rejects sanitized input during document deserialization', () => {
    expect(() => deserializeControlledDocument('<script>alert(1)</script>')).toThrow(
      UnsafeControlledHtmlError,
    );
  });

  it('rejects untrusted CSS and excessive HTML nesting', () => {
    const document = createEmptyDocument({
      root_id: nodeId('root'),
      title: 'Safe',
      language: 'en',
    });
    const serialized = serializeControlledDocument(document);

    expect(() =>
      deserializeSafeHtmlDocument({ ...serialized, css: '@import url(https://evil.test/x);' }),
    ).toThrow('Controlled document CSS does not match the policy.');

    const deepHtml = `${'<div>'.repeat(101)}safe${'</div>'.repeat(101)}`;
    expect(applyControlledHtmlPolicy(deepHtml).issues).toEqual(
      expect.arrayContaining([expect.objectContaining({ code: 'HTML_DEPTH_LIMIT_EXCEEDED' })]),
    );
  });
});
