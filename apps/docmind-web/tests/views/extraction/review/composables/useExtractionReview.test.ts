// @vitest-environment happy-dom

import type { ExtractionRunStatus, ExtractionRunView, SourcePreviewAccess } from '@/contracts';
import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useExtractionReview } from '@/views/extraction/review/composables/useExtractionReview.js';

const mocks = vi.hoisted(() => ({
  approveExtraction: vi.fn(),
  eventError: undefined as (() => void) | undefined,
  getAuthenticatedObjectUrl: vi.fn(),
  getExtraction: vi.fn(),
  getSourcePreview: vi.fn(),
  reviewExtractionField: vi.fn(),
  stopEvents: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { extractionId: 'run-1' } }),
}));

vi.mock('@/stores/auth.js', () => ({
  useAuthStore: () => ({ accessToken: 'test-token' }),
}));

vi.mock('@/api/client.js', () => ({
  getAuthenticatedObjectUrl: mocks.getAuthenticatedObjectUrl,
}));

vi.mock('@/api/extractions.js', () => ({
  approveExtraction: mocks.approveExtraction,
  connectExtractionEvents: (
    _extractionId: string,
    _accessToken: string,
    _onEvent: () => void,
    onError: () => void,
  ) => {
    mocks.eventError = onError;
    return mocks.stopEvents;
  },
  getExtraction: mocks.getExtraction,
  reviewExtractionField: mocks.reviewExtractionField,
}));

vi.mock('@/api/sources.js', () => ({
  getSourcePreview: mocks.getSourcePreview,
}));

const createRun = (status: ExtractionRunStatus): ExtractionRunView =>
  ({
    id: 'run-1',
    source_version_id: 'source-version-1',
    status,
    result: { fields: [] },
  }) as unknown as ExtractionRunView;

const createPreview = (status: 'failed' | 'ready'): SourcePreviewAccess =>
  ({
    preview: { status },
    view_url: status === 'ready' ? '/preview.pdf' : null,
    original_content_url: '/original.docx',
  }) as unknown as SourcePreviewAccess;

const mountReview = () => {
  let review: ReturnType<typeof useExtractionReview> | undefined;
  const wrapper = mount(
    defineComponent({
      setup() {
        review = useExtractionReview();
        return () => null;
      },
    }),
  );
  if (review === undefined) throw new Error('review composable was not initialized');
  return { review, wrapper };
};

describe('useExtractionReview resource lifecycle', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    mocks.eventError = undefined;
    mocks.getAuthenticatedObjectUrl.mockResolvedValue('blob:preview');
    mocks.getSourcePreview.mockResolvedValue(createPreview('failed'));
  });

  it('falls back to polling and cancels the scheduled work when unmounted', async () => {
    mocks.getExtraction.mockResolvedValue(createRun('running'));
    const { review, wrapper } = mountReview();
    await flushPromises();

    expect(mocks.eventError).toBeTypeOf('function');
    mocks.eventError?.();
    expect(review.connectionNotice.value).toContain('自动切换为状态轮询');

    await vi.advanceTimersByTimeAsync(500);
    expect(mocks.getExtraction).toHaveBeenCalledTimes(2);

    wrapper.unmount();
    expect(mocks.stopEvents).toHaveBeenCalledOnce();
    await vi.advanceTimersByTimeAsync(3000);
    expect(mocks.getExtraction).toHaveBeenCalledTimes(2);
  });

  it('revokes the authenticated preview object URL when unmounted', async () => {
    const revokeObjectUrl = vi.spyOn(URL, 'revokeObjectURL');
    mocks.getExtraction.mockResolvedValue(createRun('review_required'));
    mocks.getSourcePreview.mockResolvedValue(createPreview('ready'));
    const { review, wrapper } = mountReview();
    await flushPromises();

    expect(review.previewObjectUrl.value).toBe('blob:preview');
    wrapper.unmount();
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:preview');
  });
});
