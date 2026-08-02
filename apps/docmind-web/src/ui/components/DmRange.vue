<script setup lang="ts">
withDefaults(
  defineProps<{
    id?: string;
    min?: number;
    max?: number;
    step?: number;
    disabled?: boolean;
    tone?: 'brand' | 'success' | 'warning';
  }>(),
  {
    min: 0,
    max: 100,
    step: 1,
    disabled: false,
    tone: 'brand',
  },
);

const modelValue = defineModel<number>({ default: 0 });

const handleInput = (event: Event): void => {
  modelValue.value = Number((event.target as HTMLInputElement).value);
};
</script>

<template>
  <input
    :id="id"
    :value="modelValue"
    type="range"
    :min="min"
    :max="max"
    :step="step"
    :disabled="disabled"
    :class="[
      'dm-range h-1.5 w-full cursor-pointer appearance-none rounded-full bg-zinc-200 disabled:cursor-not-allowed disabled:opacity-50',
      tone === 'success'
        ? 'accent-emerald-500'
        : tone === 'warning'
          ? 'accent-amber-500'
          : 'accent-brand-600',
    ]"
    @input="handleInput"
  />
</template>
