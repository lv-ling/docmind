<script setup lang="ts">
import { ref } from 'vue';

import AppIcon from '@/components/AppIcon.vue';
import { DmButton, DmInput } from '@/ui';

defineProps<{
  account: string;
  password: string;
  error: string;
  isSubmitting: boolean;
  sessionExpired: boolean;
}>();

const emit = defineEmits<{
  'update:account': [value: string];
  'update:password': [value: string];
  submit: [];
}>();

const isPasswordVisible = ref(false);
</script>

<template>
  <section class="flex min-w-0 items-center justify-center bg-paper" aria-labelledby="login-title">
    <div class="w-full max-w-85">
      <div
        class="mb-8 inline-flex translate-y-2 items-center gap-1.5 rounded-compact border border-zinc-200 bg-zinc-50 px-2.5 py-1.5 text-[0.6875rem] font-medium text-zinc-500 opacity-0 shadow-subtle animate-dm-reveal motion-reduce:translate-y-0 motion-reduce:animate-none motion-reduce:opacity-100"
      >
        <AppIcon name="building-2" class="size-3 text-zinc-400" />
        <span>DocMind 企业版</span>
      </div>

      <header
        class="mb-8 translate-y-2 opacity-0 animate-dm-reveal animate-delay-100 motion-reduce:translate-y-0 motion-reduce:animate-none motion-reduce:opacity-100"
      >
        <h2
          id="login-title"
          class="mt-0 mr-0 mb-2 ml-0 text-[1.5rem] leading-[1.33] font-semibold tracking-[-0.025em] text-zinc-900"
        >
          进入 DocMind 工作空间
        </h2>
        <p class="m-0 text-[0.8125rem] text-zinc-500">使用企业账号进行身份验证</p>
      </header>

      <form
        class="grid translate-y-2 gap-5 opacity-0 animate-dm-reveal animate-delay-150 motion-reduce:translate-y-0 motion-reduce:animate-none motion-reduce:opacity-100"
        @submit.prevent="emit('submit')"
      >
        <div
          v-if="sessionExpired"
          id="login-session-message"
          class="rounded-compact border border-danger-border bg-danger-soft px-3 py-2.5 text-[0.75rem] leading-[1.4] text-danger-strong"
          role="status"
        >
          会话已过期，请重新登录
        </div>
        <div
          v-if="error"
          id="login-error"
          class="rounded-compact border border-danger-border bg-danger-soft px-3 py-2.5 text-[0.75rem] leading-[1.4] text-danger-strong"
          role="alert"
        >
          {{ error }}
        </div>

        <label class="grid gap-1.5" for="login-account">
          <span class="text-[0.75rem] font-medium text-zinc-700">企业账号</span>
          <DmInput
            id="login-account"
            appearance="unstyled"
            class="min-h-10 w-full rounded-compact border border-zinc-200 bg-paper px-3.5 py-2.5 text-[0.8125rem] leading-5 text-zinc-900 shadow-subtle outline-none transition-[border-color,box-shadow] duration-control ease-dm placeholder:text-zinc-400 focus:border-brand-400 focus:shadow-control-focus motion-reduce:transition-none"
            :model-value="account"
            type="text"
            name="account"
            autocomplete="username"
            placeholder="name@company.com"
            required
            :aria-describedby="error ? 'login-error' : undefined"
            @update:model-value="emit('update:account', String($event))"
          />
        </label>

        <div class="grid gap-1.5">
          <label class="text-[0.75rem] font-medium text-zinc-700" for="login-password">
            密码
          </label>
          <div class="relative">
            <DmInput
              id="login-password"
              appearance="unstyled"
              class="min-h-10 w-full rounded-compact border border-zinc-200 bg-paper py-2.5 pr-11 pl-3.5 text-[0.8125rem] leading-5 text-zinc-900 shadow-subtle outline-none transition-[border-color,box-shadow] duration-control ease-dm placeholder:text-zinc-400 focus:border-brand-400 focus:shadow-control-focus motion-reduce:transition-none"
              :model-value="password"
              :type="isPasswordVisible ? 'text' : 'password'"
              name="password"
              autocomplete="current-password"
              placeholder="••••••••"
              required
              :aria-describedby="error ? 'login-error' : undefined"
              @update:model-value="emit('update:password', String($event))"
            />
            <DmButton
              variant="ghost"
              icon-only
              class="absolute top-1/2 right-2 inline-flex size-7 -translate-y-1/2 cursor-pointer items-center justify-center rounded-xs border-0 bg-transparent p-0 text-zinc-400 transition-[color,background-color] duration-interaction ease-standard hover:bg-zinc-100 hover:text-zinc-700 focus-visible:text-brand-600 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-brand-focus motion-reduce:transition-none"
              :aria-label="isPasswordVisible ? '隐藏密码' : '显示密码'"
              :aria-pressed="isPasswordVisible"
              @click="isPasswordVisible = !isPasswordVisible"
            >
              <AppIcon :name="isPasswordVisible ? 'eye-off' : 'eye'" class="size-4" />
            </DmButton>
          </div>
        </div>

        <div class="pt-2">
          <DmButton
            variant="dark"
            class="inline-flex min-h-10 w-full cursor-pointer items-center justify-center gap-2 rounded-compact border-0 bg-zinc-900 px-4 py-2.5 text-[0.8125rem] leading-5 font-medium text-white shadow-subtle transition-colors duration-interaction ease-standard enabled:hover:bg-zinc-800 focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-brand-focus disabled:cursor-wait motion-reduce:transition-none"
            type="submit"
            :disabled="isSubmitting"
            :aria-busy="isSubmitting"
          >
            <template v-if="isSubmitting">
              <AppIcon
                name="loader-circle"
                class="size-3.5 animate-dm-spin motion-reduce:animate-none"
              />
              <span>验证中...</span>
            </template>
            <template v-else>
              <span>登录工作空间</span>
              <AppIcon name="arrow-right" class="size-3.5" />
            </template>
          </DmButton>
        </div>
      </form>

      <p
        class="mt-8 mr-0 mb-0 ml-0 flex translate-y-2 items-center justify-center gap-1.5 border-t border-zinc-100 pt-6 text-[0.6875rem] text-zinc-400 opacity-0 animate-dm-reveal animate-delay-200 motion-reduce:translate-y-0 motion-reduce:animate-none motion-reduce:opacity-100"
      >
        <AppIcon name="shield-check" class="size-3.5 text-zinc-300" />
        <span>安全连接 · 会话仅保留在当前浏览器标签页</span>
      </p>
    </div>
  </section>
</template>

<style scoped>
.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  background: #fff;
}

.login-panel__content {
  width: 100%;
  max-width: 21.25rem;
}

.login-panel__workspace {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.625rem;
  margin-bottom: 2rem;
  color: var(--login-zinc-500);
  border: 1px solid var(--login-zinc-200);
  border-radius: 0.375rem;
  background: var(--login-zinc-50);
  box-shadow: 0 1px 2px rgb(0 0 0 / 5%);
  font-size: 0.6875rem;
  font-weight: 500;
}

.login-panel__workspace svg {
  color: var(--login-zinc-400);
}

.login-panel__header {
  margin-bottom: 2rem;
}

.login-panel__header h2 {
  margin: 0 0 0.5rem;
  color: var(--login-zinc-900);
  font-size: 1.5rem;
  font-weight: 600;
  line-height: 1.33;
  letter-spacing: -0.025em;
}

.login-panel__header p {
  margin: 0;
  color: var(--login-zinc-500);
  font-size: 0.8125rem;
}

.login-form {
  display: grid;
  gap: 1.25rem;
}

.login-form__notice {
  padding: 0.625rem 0.75rem;
  color: #b91c1c;
  border: 1px solid #fecaca;
  border-radius: 0.375rem;
  background: #fef2f2;
  font-size: 0.75rem;
  line-height: 1.4;
}

.login-form__field {
  display: grid;
  gap: 0.375rem;
}

.login-form__field > span,
.login-form__field > label {
  color: var(--login-zinc-700);
  font-size: 0.75rem;
  font-weight: 500;
}

.login-form__password-control {
  position: relative;
}

.login-form__field input {
  width: 100%;
  min-height: 2.5rem;
  padding: 0.625rem 0.875rem;
  color: var(--login-zinc-900);
  border: 1px solid var(--login-zinc-200);
  border-radius: 0.375rem;
  outline: none;
  background: #fff;
  box-shadow: 0 1px 2px rgb(0 0 0 / 5%);
  font-size: 0.8125rem;
  line-height: 1.25rem;
  transition:
    border-color 200ms var(--login-easing),
    box-shadow 200ms var(--login-easing);
}

.login-form__field input::placeholder {
  color: var(--login-zinc-400);
}

.login-form__field input:focus {
  border-color: var(--login-indigo-400);
  box-shadow: 0 0 0 4px rgb(224 231 255 / 50%);
}

.login-form__password-control input {
  padding-right: 2.75rem;
}

.login-form__password-toggle {
  position: absolute;
  top: 50%;
  right: 0.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  padding: 0;
  color: var(--login-zinc-400);
  border: 0;
  border-radius: 0.25rem;
  background: transparent;
  cursor: pointer;
  transform: translateY(-50%);
  transition:
    color 150ms ease,
    background-color 150ms ease;
}

.login-form__password-toggle:hover {
  color: var(--login-zinc-700);
  background: var(--login-zinc-100);
}

.login-form__password-toggle:focus-visible {
  color: var(--login-indigo-600);
  outline: 2px solid rgb(99 102 241 / 35%);
  outline-offset: 1px;
}

.login-form__action {
  padding-top: 0.5rem;
}

.login-form__action button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  min-height: 2.5rem;
  padding: 0.625rem 1rem;
  color: #fff;
  border: 0;
  border-radius: 0.375rem;
  background: var(--login-zinc-900);
  box-shadow: 0 1px 2px rgb(0 0 0 / 5%);
  font-size: 0.8125rem;
  font-weight: 500;
  line-height: 1.25rem;
  cursor: pointer;
  transition: background 150ms ease;
}

.login-form__action button:hover:not(:disabled) {
  background: var(--login-zinc-800);
}

.login-form__action button:focus-visible {
  outline: 3px solid rgb(99 102 241 / 35%);
  outline-offset: 2px;
}

.login-form__action button:disabled {
  cursor: wait;
}

.login-form__spinner {
  animation: login-spinner 1s linear infinite;
}

.login-panel__security {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding-top: 1.5rem;
  margin: 2rem 0 0;
  color: var(--login-zinc-400);
  border-top: 1px solid var(--login-zinc-100);
  font-size: 0.6875rem;
}

.login-panel__security svg {
  color: var(--login-zinc-300);
}

.login-reveal {
  opacity: 0;
  transform: translateY(0.5rem);
  animation: login-reveal-element 600ms var(--login-easing) forwards;
}

.login-delay-100 {
  animation-delay: 100ms;
}

.login-delay-150 {
  animation-delay: 150ms;
}

.login-delay-200 {
  animation-delay: 200ms;
}

@keyframes login-reveal-element {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes login-spinner {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-reveal {
    opacity: 1;
    transform: none;
    animation: none;
  }

  .login-form__field input,
  .login-form__password-toggle,
  .login-form__action button {
    transition: none;
  }

  .login-form__spinner {
    animation: none;
  }
}
</style>
