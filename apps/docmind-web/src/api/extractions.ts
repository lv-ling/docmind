import type {
  AcceptedExtractionJob,
  ApproveExtractionRequest,
  CreateExtractionRequest,
  ExtractionFieldResultId,
  ExtractionRunId,
  ExtractionRunView,
  ReviewExtractionFieldRequest,
  SourceVersionId,
} from '@/contracts';

import { apiRequest, createIdempotencyKey } from './client.js';

export const createExtraction = (
  sourceVersionId: SourceVersionId,
  request: CreateExtractionRequest,
): Promise<AcceptedExtractionJob> =>
  apiRequest(`/api/v1/source-versions/${sourceVersionId}/extractions`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const getExtraction = (extractionId: ExtractionRunId): Promise<ExtractionRunView> =>
  apiRequest(`/api/v1/extractions/${extractionId}`);

export const reviewExtractionField = (
  extractionId: ExtractionRunId,
  fieldResultId: ExtractionFieldResultId,
  request: ReviewExtractionFieldRequest,
): Promise<ExtractionRunView> =>
  apiRequest(`/api/v1/extractions/${extractionId}/fields/${fieldResultId}`, {
    method: 'PATCH',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export const approveExtraction = (
  extractionId: ExtractionRunId,
  request: ApproveExtractionRequest,
): Promise<ExtractionRunView> =>
  apiRequest(`/api/v1/extractions/${extractionId}/approve`, {
    method: 'POST',
    body: request,
    idempotencyKey: createIdempotencyKey(),
  });

export type ExtractionEventHandler = (event: { type: string; data: unknown }) => void;

export const connectExtractionEvents = (
  extractionId: ExtractionRunId,
  accessToken: string,
  onEvent: ExtractionEventHandler,
  onUnavailable: () => void,
): (() => void) => {
  const controller = new AbortController();
  const consume = async (): Promise<void> => {
    try {
      const response = await fetch(`/api/v1/extractions/${extractionId}/events`, {
        headers: { Accept: 'text/event-stream', Authorization: `Bearer ${accessToken}` },
        signal: controller.signal,
      });
      if (!response.ok || response.body === null) throw new Error('SSE unavailable');
      const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
      let buffer = '';
      while (true) {
        const chunk = await reader.read();
        if (chunk.done) break;
        buffer += chunk.value;
        const frames = buffer.split('\n\n');
        buffer = frames.pop() ?? '';
        frames.forEach((frame) => {
          let type = 'message';
          const data: string[] = [];
          frame.split('\n').forEach((line) => {
            if (line.startsWith('event:')) type = line.slice(6).trim();
            if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
          });
          if (data.length === 0) return;
          const raw = data.join('\n');
          try {
            onEvent({ type, data: JSON.parse(raw) as unknown });
          } catch {
            onEvent({ type, data: raw });
          }
        });
      }
    } catch {
      if (!controller.signal.aborted) onUnavailable();
    }
  };
  void consume();
  return () => controller.abort();
};
