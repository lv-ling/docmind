// @vitest-environment happy-dom

import type { ExtractionRunId } from '@docmind/contracts';
import { describe, expect, it, vi } from 'vitest';

import { connectExtractionEvents } from './extractions.js';

describe('authenticated extraction event stream', () => {
  it('uses an authorization header and parses SSE frames', async () => {
    const encoder = new TextEncoder();
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(encoder.encode('event: status\ndata: {"status":"running"}\n\n'));
        controller.close();
      },
    });
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_path: string, init: RequestInit) => {
        expect(new Headers(init.headers).get('Authorization')).toBe('Bearer token-not-in-url');
        return new Response(stream, {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        });
      }),
    );
    const received = vi.fn();
    const unavailable = vi.fn();

    const stop = connectExtractionEvents(
      '00000000-0000-0000-0000-000000000001' as ExtractionRunId,
      'token-not-in-url',
      received,
      unavailable,
    );
    await vi.waitFor(() =>
      expect(received).toHaveBeenCalledWith({ type: 'status', data: { status: 'running' } }),
    );
    expect(unavailable).not.toHaveBeenCalled();
    stop();
    vi.unstubAllGlobals();
  });
});
