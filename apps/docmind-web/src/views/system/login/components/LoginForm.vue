<script setup lang="ts">
import { ArrowRight, Building2, Eye, EyeOff, LoaderCircle, ShieldCheck } from 'lucide-vue-next';
import { ref } from 'vue';

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

const handleAccountInput = (event: Event): void => {
  emit('update:account', (event.target as HTMLInputElement).value);
};

const handlePasswordInput = (event: Event): void => {
  emit('update:password', (event.target as HTMLInputElement).value);
};
</script>

<template>
  <section class="login-panel" aria-labelledby="login-title">
    <div class="login-panel__content">
      <div class="login-panel__workspace login-reveal">
        <Building2 :size="12" :stroke-width="2" aria-hidden="true" />
        <span>DocMind 企业版</span>
      </div>

      <header class="login-panel__header login-reveal login-delay-100">
        <h2 id="login-title">进入 DocMind 工作空间</h2>
        <p>使用企业账号进行身份验证</p>
      </header>

      <form class="login-form login-reveal login-delay-150" @submit.prevent="emit('submit')">
        <div
          v-if="sessionExpired"
          id="login-session-message"
          class="login-form__notice"
          role="status"
        >
          会话已过期，请重新登录
        </div>
        <div v-if="error" id="login-error" class="login-form__notice" role="alert">
          {{ error }}
        </div>

        <label class="login-form__field" for="login-account">
          <span>企业账号</span>
          <input
            id="login-account"
            :value="account"
            type="text"
            name="account"
            autocomplete="username"
            placeholder="name@company.com"
            required
            :aria-describedby="error ? 'login-error' : undefined"
            @input="handleAccountInput"
          />
        </label>

        <div class="login-form__field">
          <label for="login-password">密码</label>
          <div class="login-form__password-control">
            <input
              id="login-password"
              :value="password"
              :type="isPasswordVisible ? 'text' : 'password'"
              name="password"
              autocomplete="current-password"
              placeholder="••••••••"
              required
              :aria-describedby="error ? 'login-error' : undefined"
              @input="handlePasswordInput"
            />
            <button
              class="login-form__password-toggle"
              type="button"
              :aria-label="isPasswordVisible ? '隐藏密码' : '显示密码'"
              :aria-pressed="isPasswordVisible"
              @click="isPasswordVisible = !isPasswordVisible"
            >
              <EyeOff v-if="isPasswordVisible" :size="16" :stroke-width="1.8" />
              <Eye v-else :size="16" :stroke-width="1.8" />
            </button>
          </div>
        </div>

        <div class="login-form__action">
          <button type="submit" :disabled="isSubmitting" :aria-busy="isSubmitting">
            <template v-if="isSubmitting">
              <LoaderCircle class="login-form__spinner" :size="14" :stroke-width="2" />
              <span>验证中...</span>
            </template>
            <template v-else>
              <span>登录工作空间</span>
              <ArrowRight :size="14" :stroke-width="2" aria-hidden="true" />
            </template>
          </button>
        </div>
      </form>

      <p class="login-panel__security login-reveal login-delay-200">
        <ShieldCheck :size="14" :stroke-width="2" aria-hidden="true" />
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
