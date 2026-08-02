import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  BLOCK_NODE_TYPES,
  DEFAULT_PAGE_LAYOUT,
  DOCUMENT_MODEL_VERSION,
  INLINE_NODE_TYPES,
  createEmptyDocument,
  type ControlledDocument,
  type DocumentNodeId,
  type HeaderFooterRegion,
  type TemplatePlaceholderNode,
} from '@/editor';

const nodeId = (value: string): DocumentNodeId => value as DocumentNodeId;

describe('controlled document model', () => {
  it('covers the required document structures with a versioned root', () => {
    expect(DOCUMENT_MODEL_VERSION).toBe('1.0');
    expect(BLOCK_NODE_TYPES).toEqual(
      expect.arrayContaining([
        'paragraph',
        'heading',
        'list',
        'table',
        'image',
        'table_of_contents',
        'page_break',
        'page_marker',
      ]),
    );
    expect(INLINE_NODE_TYPES).toContain('dynamic_field');
    expectTypeOf<HeaderFooterRegion['variant']>().toEqualTypeOf<
      'default' | 'first_page' | 'even_pages'
    >();
  });

  it('creates independent A4 document roots without shared mutable layout state', () => {
    const first = createEmptyDocument({ root_id: nodeId('root-1'), title: 'A', language: 'zh-CN' });
    const second = createEmptyDocument({
      root_id: nodeId('root-2'),
      title: 'B',
      language: 'en-US',
    });

    first.page_layout.margins.top.value = 10;

    expect(first.model_version).toBe(DOCUMENT_MODEL_VERSION);
    expect(second.page_layout.margins.top.value).toBe(25.4);
    expect(DEFAULT_PAGE_LAYOUT.margins.top.value).toBe(25.4);
    expect(first.blocks).toEqual([]);
  });

  it('does not expose arbitrary HTML on controlled text and placeholder nodes', () => {
    expectTypeOf<TemplatePlaceholderNode>().not.toHaveProperty('html');
    expectTypeOf<ControlledDocument>().not.toHaveProperty('html');
  });
});
