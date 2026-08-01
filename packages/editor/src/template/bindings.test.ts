import type { SchemaFieldId, SchemaVersionId } from '@docmind/contracts';
import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  TEMPLATE_BINDING_RULES,
  collectTemplateBindings,
  createEmptyDocument,
  type DocumentNodeId,
  type TemplateBindingId,
  type TemplateFieldBinding,
} from '../index.js';

const nodeId = (value: string): DocumentNodeId => value as DocumentNodeId;
const bindingId = (value: string): TemplateBindingId => value as TemplateBindingId;
const schemaVersionId = (value: string): SchemaVersionId => value as SchemaVersionId;
const schemaFieldId = (value: string): SchemaFieldId => value as SchemaFieldId;

const createBinding = (
  id: string,
  fieldId: string,
  path: string,
  valueType: TemplateFieldBinding['value_type'],
): TemplateFieldBinding => ({
  id: bindingId(id),
  schema_version_id: schemaVersionId('schema-v1'),
  schema_field_id: schemaFieldId(fieldId),
  json_path: path,
  value_type: valueType,
  path_scope: 'absolute',
  value_policy: 'plain_text',
  missing_behavior: 'show_label',
  format: null,
});

describe('template bindings', () => {
  it('forbids expressions and raw HTML by construction', () => {
    expect(TEMPLATE_BINDING_RULES).toEqual({
      allows_executable_expressions: false,
      allows_raw_html_values: false,
      requires_schema_version_match: true,
      requires_unique_binding_ids: true,
    });
    expectTypeOf<TemplateFieldBinding>().not.toHaveProperty('expression');
    expectTypeOf<TemplateFieldBinding['value_policy']>().toEqualTypeOf<
      'plain_text' | 'sanitized_rich_text' | 'resource_reference'
    >();
  });

  it('collects bindings from headers, body placeholders, tables, and repeat blocks', () => {
    const headerBinding = createBinding('binding-header', 'field-header', '$.header', 'string');
    const bodyBinding = createBinding('binding-body', 'field-body', '$.party.name', 'string');
    const tableBinding = createBinding('binding-table', 'field-table', '$.amount', 'number');
    const repeatBinding = createBinding('binding-repeat', 'field-repeat', '$.items', 'array');
    const document = createEmptyDocument({
      root_id: nodeId('root'),
      title: 'Contract',
      language: 'zh-CN',
      template_schema_version_id: schemaVersionId('schema-v1'),
    });

    document.headers = [
      {
        variant: 'default',
        blocks: [
          {
            id: nodeId('header-paragraph'),
            type: 'paragraph',
            source: null,
            attributes: {},
            style: {},
            content: [
              {
                id: nodeId('header-placeholder'),
                type: 'template_placeholder',
                source: null,
                attributes: {},
                binding: headerBinding,
                label: 'Header',
                style: {},
              },
            ],
          },
        ],
      },
    ];
    document.blocks = [
      {
        id: nodeId('body-paragraph'),
        type: 'paragraph',
        source: null,
        attributes: {},
        style: {},
        content: [
          {
            id: nodeId('body-placeholder'),
            type: 'template_placeholder',
            source: null,
            attributes: {},
            binding: bodyBinding,
            label: 'Party name',
            style: {},
          },
        ],
      },
      {
        id: nodeId('table'),
        type: 'table',
        source: null,
        attributes: {},
        rows: [
          {
            id: nodeId('row'),
            type: 'table_row',
            source: null,
            attributes: {},
            is_header: false,
            height: null,
            cells: [
              {
                id: nodeId('cell'),
                type: 'table_cell',
                source: null,
                attributes: {},
                row_span: 1,
                column_span: 1,
                width: null,
                style: {},
                blocks: [
                  {
                    id: nodeId('cell-paragraph'),
                    type: 'paragraph',
                    source: null,
                    attributes: {},
                    style: {},
                    content: [
                      {
                        id: nodeId('cell-placeholder'),
                        type: 'template_placeholder',
                        source: null,
                        attributes: {},
                        binding: tableBinding,
                        label: 'Amount',
                        style: {},
                      },
                    ],
                  },
                ],
              },
            ],
          },
        ],
        width: null,
        layout: 'auto',
        repeat_header: false,
      },
      {
        id: nodeId('repeat'),
        type: 'template_repeat',
        source: null,
        attributes: {},
        binding: { ...repeatBinding, value_type: 'array' },
        item_alias: 'item',
        blocks: [],
        min_items: 0,
        max_items: null,
      },
    ];

    expect(collectTemplateBindings(document).map(({ binding }) => binding.json_path)).toEqual([
      '$.header',
      '$.party.name',
      '$.amount',
      '$.items',
    ]);
  });
});
