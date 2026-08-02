import { computed, onScopeDispose, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ApiClientError } from '@/api/client.js';
import { RouteName } from '@/router/constants.js';
import { isSafeAppRedirect } from '@/session.js';
import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

type LoginStatus = 'idle' | 'leaving' | 'submitting';

const LOGIN_EXIT_DELAY_MS = 700;

export const useLogin = () => {
  const account = ref('');
  const password = ref('');
  const error = ref('');
  const status = ref<LoginStatus>('idle');
  const pendingTimeouts = new Map<ReturnType<typeof setTimeout>, () => void>();

  const auth = useAuthStore();
  const workspace = useWorkspaceStore();
  const route = useRoute();
  const router = useRouter();

  const isLeaving = computed(() => status.value === 'leaving');
  const isSubmitting = computed(() => status.value !== 'idle');
  const sessionExpired = computed(() => route.query.reason === 'expired');

  const wait = (duration: number): Promise<void> =>
    new Promise((resolve) => {
      const timeoutId = setTimeout(() => {
        pendingTimeouts.delete(timeoutId);
        resolve();
      }, duration);
      pendingTimeouts.set(timeoutId, resolve);
    });

  const submit = async (): Promise<void> => {
    if (isSubmitting.value) return;

    error.value = '';
    status.value = 'submitting';

    try {
      await auth.login(account.value.trim(), password.value);
      await workspace.load();

      status.value = 'leaving';
      await wait(LOGIN_EXIT_DELAY_MS);

      const requested = isSafeAppRedirect(route.query.redirect) ? route.query.redirect : null;
      await router.replace(requested ?? { name: RouteName.SourceList });
    } catch (caught) {
      error.value =
        caught instanceof ApiClientError && caught.status === 401
          ? '邮箱或密码不正确'
          : caught instanceof Error
            ? caught.message
            : '登录失败，请稍后重试';
      status.value = 'idle';
    }
  };

  onScopeDispose(() => {
    for (const [timeoutId, resolve] of pendingTimeouts) {
      clearTimeout(timeoutId);
      resolve();
    }
    pendingTimeouts.clear();
  });

  return {
    account,
    error,
    isLeaving,
    isSubmitting,
    password,
    sessionExpired,
    submit,
  };
};
