import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { clearToasts, dismissToast, showToast, useToast } from '@/ui';

describe('toast queue', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    clearToasts();
  });

  afterEach(() => {
    clearToasts();
    vi.useRealTimers();
  });

  it('adds and manually dismisses a toast', () => {
    const toastId = showToast('归档成功', { tone: 'success', duration: 0 });

    expect(useToast().toasts.value).toEqual([
      { id: toastId, message: '归档成功', tone: 'success' },
    ]);

    dismissToast(toastId);
    expect(useToast().toasts.value).toEqual([]);
  });

  it('dismisses a toast after the prototype duration', () => {
    showToast('处理完成');
    expect(useToast().toasts.value).toHaveLength(1);

    vi.advanceTimersByTime(2500);
    expect(useToast().toasts.value).toHaveLength(0);
  });
});
