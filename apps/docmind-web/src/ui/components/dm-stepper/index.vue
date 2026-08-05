<script setup lang="ts">
export interface DmStepperItem {
  label: string;
  description?: string;
}

defineOptions({ name: 'DmStepper' });

withDefaults(
  defineProps<{
    items: DmStepperItem[];
    currentIndex: number;
    ariaLabel?: string;
  }>(),
  {
    ariaLabel: '处理进度',
  },
);
</script>

<template>
  <ol
    class="dm-stepper"
    :aria-label="ariaLabel"
    :style="{ '--dm-step-count': Math.max(items.length, 1) }"
  >
    <li
      v-for="(item, index) in items"
      :key="`${index}-${item.label}`"
      :class="{
        'dm-stepper__item--complete': index < currentIndex,
        'dm-stepper__item--current': index === currentIndex,
      }"
      :aria-current="index === currentIndex ? 'step' : undefined"
    >
      <span class="dm-stepper__marker" aria-hidden="true">{{ index + 1 }}</span>
      <span class="dm-stepper__copy">
        <strong>{{ item.label }}</strong>
        <small v-if="item.description">{{ item.description }}</small>
      </span>
    </li>
  </ol>
</template>
