import type { UserSummary } from '@docmind/contracts';
import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import { configureApiClient } from '../api/client.js';
import { getCurrentUser, login as loginRequest } from '../api/identity.js';
import { SESSION_EXPIRED_EVENT } from '../session.js';

const TOKEN_KEY = 'docmind.access-token';

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(sessionStorage.getItem(TOKEN_KEY));
  const user = ref<UserSummary | null>(null);
  const initialized = ref(false);
  const isAuthenticated = computed(() => accessToken.value !== null);

  const clear = (notifyExpired = false): void => {
    accessToken.value = null;
    user.value = null;
    sessionStorage.removeItem(TOKEN_KEY);
    if (notifyExpired) window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
  };

  configureApiClient(
    () => accessToken.value,
    () => clear(true),
  );

  const login = async (email: string, password: string): Promise<void> => {
    const response = await loginRequest({ email, password });
    accessToken.value = response.access_token;
    user.value = response.user;
    sessionStorage.setItem(TOKEN_KEY, response.access_token);
  };

  const initialize = async (): Promise<void> => {
    if (initialized.value) return;
    if (accessToken.value !== null) {
      try {
        user.value = await getCurrentUser();
      } catch {
        clear();
      }
    }
    initialized.value = true;
  };

  const logout = (): void => clear();

  return { accessToken, user, initialized, isAuthenticated, login, logout, initialize };
});
