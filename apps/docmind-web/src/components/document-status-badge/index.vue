<script setup lang="ts">
import { computed } from 'vue';

import { DmStatus } from '@/ui';

type StatusTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

defineOptions({ name: 'DocumentStatusBadge' });

const props = defineProps<{
  status: string;
  label?: string;
}>();

const statusLabels: Record<string, string> = {
  uploading: '上传中',
  verifying: '安全校验中',
  previewing: '生成预览中',
  ready: '可用',
  validation_failed: '校验失败',
  preview_failed: '预览失败',
  draft: '草稿',
  processing: '处理中',
  completed: '已完成',
};

const statusTones: Record<string, StatusTone> = {
  uploading: 'info',
  verifying: 'info',
  previewing: 'info',
  ready: 'success',
  validation_failed: 'danger',
  preview_failed: 'warning',
  draft: 'neutral',
  processing: 'info',
  completed: 'success',
};

const displayLabel = computed(() => props.label ?? statusLabels[props.status] ?? props.status);
const tone = computed<StatusTone>(() => statusTones[props.status] ?? 'neutral');
</script>

<template>
  <DmStatus :label="displayLabel" :tone="tone" />
</template>
