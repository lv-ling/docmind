// @vitest-environment happy-dom

import type { SourceVersionId, TemplateResource, WorkspaceId } from '@/contracts';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { configureApiClient } from '@/api/client.js';
import { createTemplate, hydrateTemplateHtml, listTemplates } from '@/api/templates.js';

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  configureApiClient(
    () => null,
    () => undefined,
  );
});

describe('template API', () => {
  it('creates an idempotent source-linked conversion and lists workspace templates', async () => {
    const fetchMock = vi.fn(async (path: string, init?: RequestInit) => {
      if (path.includes('/source-versions/')) {
        const headers = new Headers(init?.headers);
        expect(init?.method).toBe('POST');
        expect(headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
        expect(init?.body).toBe('{"name":"合同模板"}');
        return new Response(
          JSON.stringify({
            job_id: crypto.randomUUID(),
            template_id: crypto.randomUUID(),
            request_id: crypto.randomUUID(),
          }),
          { status: 202, headers: { 'Content-Type': 'application/json' } },
        );
      }
      expect(path).toBe('/api/v1/workspaces/workspace-id/templates');
      return new Response('[]', {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);

    await createTemplate('source-version-id' as SourceVersionId, '合同模板');
    await expect(listTemplates('workspace-id' as WorkspaceId)).resolves.toEqual([]);
  });

  it('loads protected resources with bearer auth and rewrites safe HTML to blob URLs', async () => {
    configureApiClient(
      () => 'template-token',
      () => undefined,
    );
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_path: string, init?: RequestInit) => {
        expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer template-token');
        return new Response(new Blob(['png'], { type: 'image/png' }), { status: 200 });
      }),
    );
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:protected-resource');
    const resource = {
      id: 'resource-id',
      kind: 'image',
      content_type: 'image/png',
      byte_size: 3,
      sha256: '0'.repeat(64),
      download_url: '/api/v1/template-resources/resource-id/content',
    } as TemplateResource;

    const hydrated = await hydrateTemplateHtml(
      '<img src="/api/v1/template-resources/resource-id/content">',
      [resource],
    );

    expect(hydrated.html).toContain('src="blob:protected-resource"');
    expect(hydrated.objectUrls).toEqual(['blob:protected-resource']);
  });
});
