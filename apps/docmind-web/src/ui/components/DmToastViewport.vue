<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';
import { DmButton } from '@/components/dm';

import type { DmToastTone } from '../toast.js';
import { useToast } from '../toast.js';

const { toasts, dismissToast } = useToast();

const icons = {
  success: 'circle-check',
  info: 'info',
  warning: 'circle-alert',
  danger: 'circle-x',
} as const satisfies Record<DmToastTone, InstanceType<typeof AppIcon>['$props']['name']>;
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
        <AppIcon :name="icons[toast.tone]" class="dm-toast__icon" />
        <span>{{ toast.message }}</span>
        <DmButton variant="ghost" icon-only aria-label="关闭通知" @click="dismissToast(toast.id)">
          <AppIcon name="close" />
        </DmButton>
      </article>
    </TransitionGroup>
  </div>
</template>
