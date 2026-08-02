<script setup lang="ts">
import { CircleAlert, CircleCheck, CircleX, Info, X } from 'lucide-vue-next';

import type { DmToastTone } from '../toast.js';
import { useToast } from '../toast.js';

const { toasts, dismissToast } = useToast();

const icons = {
  success: CircleCheck,
  info: Info,
  warning: CircleAlert,
  danger: CircleX,
} satisfies Record<DmToastTone, typeof CircleCheck>;
</script>

<template>
  <div class="dm-toast-viewport" aria-live="polite" aria-relevant="additions">
    <TransitionGroup name="dm-toast">
      <article
        v-for="toast in toasts"
        :key="toast.id"
        :class="['dm-toast', `dm-toast--${toast.tone}`]"
        :role="toast.tone === 'danger' ? 'alert' : 'status'"
      >
        <component :is="icons[toast.tone]" class="dm-toast__icon" aria-hidden="true" />
        <span>{{ toast.message }}</span>
        <button type="button" aria-label="关闭通知" @click="dismissToast(toast.id)">
          <X aria-hidden="true" />
        </button>
      </article>
    </TransitionGroup>
  </div>
</template>
