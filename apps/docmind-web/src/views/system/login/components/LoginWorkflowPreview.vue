<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';
</script>

<template>
  <ol
    class="relative m-0 grid list-none gap-10 p-0 before:absolute before:top-4 before:bottom-4 before:left-3.5 before:w-px before:bg-linear-to-b before:from-zinc-200 before:via-brand-300 before:to-zinc-200 before:content-[''] translate-y-2 opacity-0 animate-dm-reveal animate-delay-200 motion-reduce:translate-y-0 motion-reduce:animate-none motion-reduce:opacity-100"
    aria-label="文档智能处理流程"
  >
    <li class="group relative flex items-start gap-6">
      <span
        class="relative z-1 inline-flex size-7 shrink-0 items-center justify-center rounded-round border border-zinc-200 bg-paper text-zinc-400 shadow-subtle transition-transform duration-page ease-dm group-hover:scale-110 motion-reduce:transition-none"
        aria-hidden="true"
      >
        <AppIcon name="scan-line" class="size-3.5" />
      </span>
      <span class="grid gap-1 pt-1">
        <strong class="text-[0.8125rem] font-medium tracking-[0.025em] text-zinc-800">
          文档深度解析
        </strong>
        <small class="font-mono text-[0.6875rem] tracking-[-0.025em] text-zinc-400">
          Document Understanding
        </small>
      </span>
    </li>
    <li class="group relative flex items-start gap-6">
      <span
        class="relative z-1 inline-flex size-7 shrink-0 items-center justify-center rounded-round border border-brand-200 bg-brand-50 text-brand-600 shadow-brand-glow transition-transform duration-page ease-dm group-hover:scale-110 motion-reduce:transition-none"
        aria-hidden="true"
      >
        <AppIcon name="sparkles" class="size-3.5" />
        <i
          class="absolute -inset-px rounded-round border border-brand-400 opacity-20 animate-dm-ping motion-reduce:animate-none"
        ></i>
      </span>
      <span class="grid gap-1 pt-1">
        <strong class="text-[0.8125rem] font-medium tracking-[0.025em] text-brand-600">
          结构化智能抽取
        </strong>
        <small class="font-mono text-[0.6875rem] tracking-[-0.025em] text-brand-muted">
          AI Extraction
        </small>
      </span>
    </li>
    <li class="group relative flex items-start gap-6">
      <span
        class="relative z-1 inline-flex size-7 shrink-0 items-center justify-center rounded-round border border-zinc-200 bg-paper text-zinc-400 shadow-subtle transition-transform duration-page ease-dm group-hover:scale-110 motion-reduce:transition-none"
        aria-hidden="true"
      >
        <AppIcon name="user-check" class="size-3.5" />
      </span>
      <span class="grid gap-1 pt-1">
        <strong class="text-[0.8125rem] font-medium tracking-[0.025em] text-zinc-800">
          人工协作复核
        </strong>
        <small class="font-mono text-[0.6875rem] tracking-[-0.025em] text-zinc-400">
          Human Review
        </small>
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
