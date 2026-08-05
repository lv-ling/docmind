<script setup lang="ts">
import { computed } from 'vue';

import { DmStepper, type DmStepperItem } from '@/ui';

export interface DocumentTaskStep {
  id: string;
  label: string;
  description?: string;
}

defineOptions({ name: 'DocumentTaskProgress' });

const props = withDefaults(
  defineProps<{
    steps: DocumentTaskStep[];
    currentStepId: string;
    ariaLabel?: string;
  }>(),
  {
    ariaLabel: '文档任务进度',
  },
);

const currentIndex = computed(() =>
  Math.max(
    0,
    props.steps.findIndex((step) => step.id === props.currentStepId),
  ),
);
const stepperItems = computed<DmStepperItem[]>(() =>
  props.steps.map(({ label, description }) => ({
    label,
    ...(description === undefined ? {} : { description }),
  })),
);
</script>

<template>
  <DmStepper :items="stepperItems" :current-index="currentIndex" :aria-label="ariaLabel" />
</template>
