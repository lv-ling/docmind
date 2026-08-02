<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import AppIcon from '@/components/AppIcon.vue';
import { Checkbox } from '@/components/ui/checkbox';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    id: string;
    label: string;
    description?: string;
    disabled?: boolean;
    class?: HTMLAttributes['class'];
  }>(),
  {
    description: '',
    disabled: false,
  },
);

const modelValue = defineModel<boolean>({ required: true });
const rootClass = computed(() =>
  cn('inline-flex min-w-0 cursor-pointer items-start gap-2 text-zinc-600', props.class),
);

const handleModelUpdate = (value: boolean | 'indeterminate'): void => {
  modelValue.value = value === true;
};
</script>

<template>
  <label :for="id" :class="rootClass">
    <Checkbox
      v-bind="$attrs"
      :id="id"
      :model-value="modelValue"
      :disabled="disabled"
      class="mt-0.5 size-3.5 rounded border border-zinc-300 bg-white text-white transition-colors data-[state=checked]:border-brand-600 data-[state=checked]:bg-brand-600 focus-visible:ring-2 focus-visible:ring-brand-400 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
      @update:model-value="handleModelUpdate"
    >
      <AppIcon name="check" class="size-3" />
    </Checkbox>
    <span class="min-w-0">
      <strong
        class="block text-[12px] leading-4 font-medium text-zinc-900 transition-colors group-hover:text-brand-600"
        >{{ label }}</strong
      >
      <small v-if="description" class="mt-1 block text-[11px] leading-relaxed text-zinc-500">
        {{ description }}
      </small>
    </span>
  </label>
</template>
