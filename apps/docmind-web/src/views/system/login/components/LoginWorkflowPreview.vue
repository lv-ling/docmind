<script setup lang="ts">
import { ScanLine, Sparkles, UserCheck } from 'lucide-vue-next';
</script>

<template>
  <ol class="login-workflow login-reveal login-delay-200" aria-label="文档智能处理流程">
    <li class="login-workflow__step">
      <span class="login-workflow__icon" aria-hidden="true">
        <ScanLine :size="14" :stroke-width="2" />
      </span>
      <span class="login-workflow__copy">
        <strong>文档深度解析</strong>
        <small>Document Understanding</small>
      </span>
    </li>
    <li class="login-workflow__step login-workflow__step--active">
      <span class="login-workflow__icon" aria-hidden="true">
        <Sparkles :size="14" :stroke-width="2" />
        <i class="login-workflow__pulse"></i>
      </span>
      <span class="login-workflow__copy">
        <strong>结构化智能抽取</strong>
        <small>AI Extraction</small>
      </span>
    </li>
    <li class="login-workflow__step">
      <span class="login-workflow__icon" aria-hidden="true">
        <UserCheck :size="14" :stroke-width="2" />
      </span>
      <span class="login-workflow__copy">
        <strong>人工协作复核</strong>
        <small>Human Review</small>
      </span>
    </li>
  </ol>
</template>

<style scoped>
.login-workflow {
  position: relative;
  display: grid;
  gap: 2.5rem;
  padding: 0;
  margin: 0;
  list-style: none;
}

.login-workflow::before {
  position: absolute;
  top: 1rem;
  bottom: 1rem;
  left: 0.875rem;
  width: 1px;
  background: linear-gradient(to bottom, var(--login-zinc-200), #a5b4fc, var(--login-zinc-200));
  content: '';
}

.login-workflow__step {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
}

.login-workflow__icon {
  position: relative;
  z-index: 1;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  color: var(--login-zinc-400);
  border: 1px solid var(--login-zinc-200);
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(0 0 0 / 5%);
  transition: transform 300ms var(--login-easing);
}

.login-workflow__step:hover .login-workflow__icon {
  transform: scale(1.1);
}

.login-workflow__copy {
  display: grid;
  gap: 0.25rem;
  padding-top: 0.25rem;
}

.login-workflow__copy strong {
  color: var(--login-zinc-800);
  font-size: 0.8125rem;
  font-weight: 500;
  letter-spacing: 0.025em;
}

.login-workflow__copy small {
  color: var(--login-zinc-400);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.6875rem;
  letter-spacing: -0.025em;
}

.login-workflow__step--active .login-workflow__icon {
  color: var(--login-indigo-600);
  border-color: var(--login-indigo-200);
  background: var(--login-indigo-50);
  box-shadow: 0 0 0.75rem rgb(99 102 241 / 15%);
}

.login-workflow__step--active .login-workflow__copy strong {
  color: var(--login-indigo-600);
}

.login-workflow__step--active .login-workflow__copy small {
  color: rgb(129 140 248 / 70%);
}

.login-workflow__pulse {
  position: absolute;
  inset: -1px;
  border: 1px solid var(--login-indigo-400);
  border-radius: inherit;
  opacity: 0.2;
  animation: login-ai-ping 1s cubic-bezier(0, 0, 0.2, 1) infinite;
}

.login-reveal {
  opacity: 0;
  transform: translateY(0.5rem);
  animation: login-reveal-element 600ms var(--login-easing) forwards;
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

@keyframes login-ai-ping {
  75%,
  100% {
    opacity: 0;
    transform: scale(2);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-reveal {
    opacity: 1;
    transform: none;
    animation: none;
  }

  .login-workflow__icon {
    transition: none;
  }

  .login-workflow__pulse {
    animation: none;
  }
}
</style>
