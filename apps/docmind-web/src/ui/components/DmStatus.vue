<script setup lang="ts">
type DmStatusTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

withDefaults(
  defineProps<{
    tone?: DmStatusTone;
    label: string;
    live?: boolean;
  }>(),
  {
    tone: 'neutral',
    live: false,
  },
);
</script>

<template>
  <span
    :class="['dm-status', `dm-status--${tone}`]"
    :role="live ? 'status' : undefined"
    :aria-live="live ? 'polite' : undefined"
  >
    <span class="dm-status__dot" aria-hidden="true"></span>
    <span class="dm-status__label">{{ label }}</span>
  </span>
</template>
