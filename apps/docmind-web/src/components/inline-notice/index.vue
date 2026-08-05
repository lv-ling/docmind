<script setup lang="ts">
defineOptions({ name: 'InlineNotice' });

withDefaults(
  defineProps<{
    tone?: 'info' | 'success' | 'warning' | 'danger';
    title: string;
    detail?: string;
  }>(),
  { tone: 'info' },
);
</script>

<template>
  <div class="inline-notice" :class="`inline-notice--${tone}`" role="status">
    <strong>{{ title }}</strong>
    <span v-if="detail">{{ detail }}</span>
  </div>
</template>
