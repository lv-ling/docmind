<script setup lang="ts">
defineOptions({ name: 'DmSkeleton' });

withDefaults(
  defineProps<{
    lines?: number;
    label?: string;
  }>(),
  {
    lines: 3,
    label: '正在加载',
  },
);
</script>

<template>
  <div class="dm-skeleton" role="status" :aria-label="label">
    <span v-for="line in lines" :key="line"></span>
  </div>
</template>
