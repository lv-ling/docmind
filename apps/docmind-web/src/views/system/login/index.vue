<script setup lang="ts">
import LoginBrandPanel from './components/LoginBrandPanel.vue';
import LoginForm from './components/LoginForm.vue';
import { useLogin } from './composables/useLogin.js';

const { account, error, isLeaving, isSubmitting, password, sessionExpired, submit } = useLogin();
</script>

<template>
  <main
    class="fixed inset-0 z-100 grid min-h-screen min-w-256 grid-cols-2 overflow-hidden bg-paper font-ui text-zinc-900 transition-opacity duration-exit ease-standard animate-dm-page-fade motion-reduce:animate-none motion-reduce:transition-none"
    :class="{ 'opacity-0': isLeaving }"
  >
    <LoginBrandPanel />
    <LoginForm
      v-model:account="account"
      v-model:password="password"
      :error="error"
      :is-submitting="isSubmitting"
      :session-expired="sessionExpired"
      @submit="submit"
    />
  </main>
</template>

<style scoped>
.login-layout {
  --login-zinc-50: #fafafa;
  --login-zinc-100: #f4f4f5;
  --login-zinc-200: #e4e4e7;
  --login-zinc-300: #d4d4d8;
  --login-zinc-400: #a1a1aa;
  --login-zinc-500: #71717a;
  --login-zinc-700: #3f3f46;
  --login-zinc-800: #27272a;
  --login-zinc-900: #18181b;
  --login-indigo-50: #eef2ff;
  --login-indigo-100: #e0e7ff;
  --login-indigo-200: #c7d2fe;
  --login-indigo-400: #818cf8;
  --login-indigo-600: #4f46e5;
  --login-easing: cubic-bezier(0.16, 1, 0.3, 1);
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 64rem;
  min-height: 100vh;
  overflow: hidden;
  color: var(--login-zinc-900);
  background: #fff;
  font-family:
    Inter,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    'Noto Sans SC',
    'Microsoft YaHei',
    system-ui,
    sans-serif;
  opacity: 1;
  transition: opacity 700ms ease;
  animation: login-page-fade 500ms var(--login-easing) both;
}

.login-layout--leaving {
  opacity: 0;
}

@keyframes login-page-fade {
  from {
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-layout {
    transition: none;
    animation: none;
  }
}
</style>
