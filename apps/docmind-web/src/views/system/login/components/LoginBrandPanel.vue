<script setup lang="ts">
import { Zap } from 'lucide-vue-next';

import LoginWorkflowPreview from './LoginWorkflowPreview.vue';
</script>

<template>
  <section class="login-brand-panel" aria-labelledby="login-brand-title">
    <div class="login-brand-panel__logo login-reveal">
      <span class="login-brand-panel__mark" aria-hidden="true">
        <Zap :size="14" :stroke-width="2" />
      </span>
      <span>DocMind</span>
    </div>

    <div class="login-brand-panel__content">
      <h1 id="login-brand-title" class="login-reveal login-delay-100">
        让复杂文档，<br />变成可信数据。
      </h1>
      <p class="login-brand-panel__tagline login-reveal login-delay-150">
        AI Document Intelligence
      </p>
      <LoginWorkflowPreview />
    </div>

    <p class="login-brand-panel__footer login-reveal login-delay-300">Enterprise AI Workspace</p>
  </section>
</template>

<style scoped>
.login-brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
  padding: 4rem;
  border-right: 1px solid rgb(228 228 231 / 60%);
  background: var(--login-zinc-50);
}

.login-brand-panel__logo {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 0.625rem;
  color: var(--login-zinc-900);
  font-size: 1.125rem;
  font-weight: 600;
  letter-spacing: -0.025em;
}

.login-brand-panel__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  color: #fff;
  border-radius: 0.25rem;
  background: var(--login-zinc-900);
  box-shadow: 0 1px 2px rgb(0 0 0 / 5%);
}

.login-brand-panel__content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 28rem;
  padding-left: 0.5rem;
  margin-block: auto;
}

.login-brand-panel__content h1 {
  margin: 0 0 1rem;
  color: var(--login-zinc-900);
  font-size: 2.25rem;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.025em;
}

.login-brand-panel__tagline {
  margin: 0 0 4rem;
  color: var(--login-zinc-400);
  font-size: 0.6875rem;
  font-weight: 500;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.login-brand-panel__footer {
  position: relative;
  z-index: 1;
  margin: 0;
  color: var(--login-zinc-400);
  font-size: 0.6875rem;
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

.login-delay-300 {
  animation-delay: 300ms;
}

@keyframes login-reveal-element {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 64rem) {
  .login-brand-panel {
    padding: 3rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-reveal {
    opacity: 1;
    transform: none;
    animation: none;
  }
}
</style>
