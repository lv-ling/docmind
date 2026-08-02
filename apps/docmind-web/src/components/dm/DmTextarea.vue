<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    id?: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    readonly?: boolean;
    rows?: number;
    maxlength?: number;
    class?: HTMLAttributes['class'];
  }>(),
  {
    placeholder: '',
    required: false,
    disabled: false,
    readonly: false,
    rows: 3,
  },
);

const modelValue = defineModel<string>({ default: '' });
const controlClass = computed(() =>
  cn(
    'dm-control min-w-0 resize-y rounded-compact border border-zinc-200 bg-white px-2.5 py-2 text-[12px] leading-5 text-zinc-800 outline-none transition-colors duration-interaction placeholder:text-zinc-400 focus:border-brand-400 focus:ring-2 focus:ring-brand-100/50 disabled:cursor-not-allowed disabled:bg-zinc-100 disabled:text-zinc-400',
    props.class,
  ),
);

const handleModelUpdate = (value: string | number): void => {
  modelValue.value = String(value);
};
</script>

<template>
  <Textarea
    v-bind="$attrs"
    :id="id"
    :model-value="modelValue"
    :placeholder="placeholder"
    :required="required"
    :disabled="disabled"
    :readonly="readonly"
    :rows="rows"
    :maxlength="maxlength"
    :class="controlClass"
    @update:model-value="handleModelUpdate"
  />
</template>
