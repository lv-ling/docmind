<script setup lang="ts">
import { computed } from 'vue';

type DmProgressTone = 'brand' | 'success' | 'warning' | 'danger' | 'neutral';
type DmProgressSize = 'small' | 'medium';

const props = withDefaults(
  defineProps<{
    value: number;
    max?: number;
    label?: string;
    tone?: DmProgressTone;
    size?: DmProgressSize;
    showValue?: boolean;
  }>(),
  {
    max: 100,
    label: '进度',
    tone: 'brand',
    size: 'medium',
    showValue: false,
  },
);

const safeMaximum = computed(() => (Number.isFinite(props.max) && props.max > 0 ? props.max : 100));
const safeValue = computed(() => Math.min(safeMaximum.value, Math.max(0, props.value)));
const percentage = computed(() => (safeValue.value / safeMaximum.value) * 100);
const displayValue = computed(() => `${Math.round(percentage.value)}%`);
</script>

<template>
  <div :class="['dm-progress', `dm-progress--${tone}`, `dm-progress--${size}`]">
    <div v-if="showValue" class="dm-progress__meta">
      <span>{{ label }}</span>
      <strong>{{ displayValue }}</strong>
    </div>
    <div
      class="dm-progress__track"
      role="progressbar"
      :aria-label="label"
      aria-valuemin="0"
      :aria-valuemax="safeMaximum"
      :aria-valuenow="safeValue"
      :aria-valuetext="displayValue"
    >
      <span class="dm-progress__indicator" :style="{ width: `${percentage}%` }"></span>
    </div>
  </div>
</template>
