<script setup lang="ts">
import { computed } from 'vue';

defineOptions({ name: 'DmProgress' });

const props = withDefaults(
  defineProps<{
    value?: number;
    label: string;
    showValue?: boolean;
  }>(),
  {
    showValue: true,
  },
);

const normalizedValue = computed(() =>
  props.value === undefined ? undefined : Math.min(100, Math.max(0, props.value)),
);
</script>

<template>
  <div class="dm-progress">
    <div class="dm-progress__label">
      <span>{{ label }}</span>
      <span v-if="showValue && normalizedValue !== undefined"
        >{{ Math.round(normalizedValue) }}%</span
      >
    </div>
    <div
      class="dm-progress__track"
      role="progressbar"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-label="label"
      :aria-valuenow="normalizedValue"
    >
      <span
        :class="[
          'dm-progress__value',
          normalizedValue === undefined ? 'dm-progress__value--indeterminate' : null,
        ]"
        :style="normalizedValue === undefined ? undefined : { width: `${normalizedValue}%` }"
      ></span>
    </div>
  </div>
</template>
