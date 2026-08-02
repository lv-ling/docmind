import type { TemplateVersion } from '@/contracts';
import { describe, expect, it } from 'vitest';

import {
  collectEditableDocumentBlocks,
  createTemplatePreviewSrcdoc,
  injectTemplateResourceUrls,
  parseTemplateDiffChanges,
} from '@/views/template/editor/model/template-document.js';

describe('template editor document model', () => {
  it('collects editable paragraphs and their source pages', () => {
    expect(
      collectEditableDocumentBlocks({
        type: 'document',
        content: [
          {
            id: 'p-1',
            type: 'paragraph',
            source: { page_number: 3 },
            content: [{ id: 't-1', type: 'text', text: '合同标题' }],
          },
        ],
      }),
    ).toEqual([{ id: 'p-1', type: 'paragraph', text: '合同标题', page: 3 }]);
  });

  it('injects authenticated resource URLs without changing unrelated HTML', () => {
    const resources = [
      { id: 'resource-1', download_url: '/api/resources/1' },
    ] as TemplateVersion['resources'];
    expect(
      injectTemplateResourceUrls(
        '<img data-dm-resource-id="resource-1"><a href="/api/resources/1">下载</a>',
        resources,
        ['blob:resource-1'],
      ),
    ).toBe(
      '<img data-dm-resource-id="resource-1" src="blob:resource-1"><a href="blob:resource-1">下载</a>',
    );
  });

  it('keeps only supported backend diff records', () => {
    expect(
      parseTemplateDiffChanges([
        { path: '$.content[0]', kind: 'changed' },
        { path: '$.content[1]', kind: 'unsupported' },
        null,
      ]),
    ).toEqual([{ path: '$.content[0]', kind: 'changed' }]);
  });

  it('creates a sandbox document with the requested zoom', () => {
    const source = createTemplatePreviewSrcdoc('<main>正文</main>', 'main{color:black}', 80);
    expect(source).toContain("default-src 'none'");
    expect(source).toContain('zoom:0.8');
    expect(source).toContain('<main>正文</main>');
  });
});
