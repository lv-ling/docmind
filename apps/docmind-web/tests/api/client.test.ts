// @vitest-environment happy-dom

import { afterEach, describe, expect, it, vi } from 'vitest';

import { ApiClientError, apiRequest, configureApiClient } from '@/api/client.js';

afterEach(() => {
  vi.unstubAllGlobals();
  configureApiClient(
    () => null,
    () => undefined,
  );
});

describe('api client', () => {
  it('adds correlation, authorization and idempotency headers', async () => {
    const fetchMock = vi.fn(async (_path: string, init: RequestInit) => {
      const headers = new Headers(init.headers);
      expect(headers.get('Authorization')).toBe('Bearer secure-token');
      expect(headers.get('Idempotency-Key')).toBe('same-operation');
      expect(headers.get('X-Request-ID')).toMatch(/^[0-9a-f-]{36}$/);
      expect(init.body).toBe('{"name":"contract"}');
      return new Response('{"id":"created"}', {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);
    configureApiClient(
      () => 'secure-token',
      () => undefined,
    );

    await expect(
      apiRequest<{ id: string }>('/api/v1/test', {
        method: 'POST',
        body: { name: 'contract' },
        idempotencyKey: 'same-operation',
      }),
    ).resolves.toEqual({ id: 'created' });
  });

  it('raises typed errors and clears auth on 401', async () => {
    const unauthorized = vi.fn();
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              code: 'AUTHENTICATION_REQUIRED',
              category: 'authentication',
              message: '会话已失效',
              details: {},
              field_errors: [],
              request_id: crypto.randomUUID(),
              timestamp: new Date().toISOString(),
            }),
            { status: 401, headers: { 'Content-Type': 'application/json' } },
          ),
      ),
    );
    configureApiClient(() => 'expired', unauthorized);

    const error = await apiRequest('/api/v1/me').catch((caught: unknown) => caught);
    expect(error).toBeInstanceOf(ApiClientError);
    expect((error as ApiClientError).response?.code).toBe('AUTHENTICATION_REQUIRED');
    expect(unauthorized).toHaveBeenCalledOnce();
  });
});
