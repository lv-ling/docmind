<script setup lang="ts">
import { computed } from 'vue';

import { DmStatus } from '@/ui';

export type DocumentSourceType =
  'source-version' | 'template-version' | 'business-document' | 'temporary-upload';

defineOptions({ name: 'DocumentSourceBadge' });

const props = defineProps<{
  type: DocumentSourceType;
}>();

const labels: Record<DocumentSourceType, string> = {
  'source-version': '不可变原件',
  'template-version': '模板版本',
  'business-document': '业务文档',
  'temporary-upload': '临时上传',
};

const label = computed(() => labels[props.type]);
</script>

<template>
  <DmStatus :label="label" :tone="type === 'business-document' ? 'info' : 'neutral'" />
</template>
