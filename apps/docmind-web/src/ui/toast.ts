import { readonly, ref } from 'vue';

export type DmToastTone = 'success' | 'info' | 'warning' | 'danger';

export interface DmToastOptions {
  tone?: DmToastTone;
  duration?: number;
}

export interface DmToastMessage {
  id: number;
  message: string;
  tone: DmToastTone;
}

const DEFAULT_TOAST_DURATION = 2500;
const toastMessages = ref<DmToastMessage[]>([]);
const toastTimers = new Map<number, ReturnType<typeof setTimeout>>();
let nextToastId = 1;

export const dismissToast = (toastId: number): void => {
  const timer = toastTimers.get(toastId);
  if (timer !== undefined) clearTimeout(timer);
  toastTimers.delete(toastId);
  toastMessages.value = toastMessages.value.filter((toast) => toast.id !== toastId);
};

export const showToast = (message: string, options: DmToastOptions = {}): number => {
  const toastId = nextToastId;
  nextToastId += 1;
  toastMessages.value = [
    ...toastMessages.value,
    { id: toastId, message, tone: options.tone ?? 'success' },
  ];

  const duration = options.duration ?? DEFAULT_TOAST_DURATION;
  if (duration > 0) {
    toastTimers.set(
      toastId,
      setTimeout(() => dismissToast(toastId), duration),
    );
  }
  return toastId;
};

export const clearToasts = (): void => {
  toastTimers.forEach((timer) => clearTimeout(timer));
  toastTimers.clear();
  toastMessages.value = [];
};

export const useToast = () => ({
  toasts: readonly(toastMessages),
  showToast,
  dismissToast,
  clearToasts,
});
