<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';

type DmInputType = 'text' | 'email' | 'tel' | 'password' | 'number' | 'date' | 'search';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    id?: string;
    type?: DmInputType;
    placeholder?: string;
    autocomplete?: string;
    required?: boolean;
    disabled?: boolean;
    readonly?: boolean;
    autofocus?: boolean;
    maxlength?: number | undefined;
    min?: string | number | undefined;
    max?: string | number | undefined;
    step?: string | number | undefined;
    appearance?: 'default' | 'unstyled';
    class?: HTMLAttributes['class'];
  }>(),
  {
    type: 'text',
    placeholder: '',
    autocomplete: 'off',
    required: false,
    disabled: false,
    readonly: false,
    autofocus: false,
    appearance: 'default',
  },
);

const [modelValue, modelModifiers] = defineModel<string | number>({ default: '' });
const emit = defineEmits<{
  blur: [event: FocusEvent];
  focus: [event: FocusEvent];
  change: [event: Event];
}>();

const controlClass = computed(() =>
  cn(
    props.appearance === 'default'
      ? 'dm-control h-8 min-w-0 rounded-compact border border-zinc-200 bg-white px-2.5 text-[12px] leading-none text-zinc-800 outline-none transition-colors duration-interaction placeholder:text-zinc-400 focus:border-brand-400 focus:ring-2 focus:ring-brand-100/50 disabled:cursor-not-allowed disabled:bg-zinc-100 disabled:text-zinc-400'
      : 'dm-control dm-control--unstyled',
    props.class,
  ),
);

const handleModelUpdate = (rawValue: string | number): void => {
  const value = modelModifiers.number
    ? Number(rawValue)
    : modelModifiers.trim
      ? String(rawValue).trim()
      : rawValue;
  modelValue.value = value;
};
</script>

<template>
  <Input
    v-bind="$attrs"
    :id="id"
    :model-value="modelValue"
    :type="type"
    :placeholder="placeholder"
    :autocomplete="autocomplete"
    :required="required"
    :disabled="disabled"
    :readonly="readonly"
    :autofocus="autofocus"
    :maxlength="maxlength"
    :min="min"
    :max="max"
    :step="step"
    :class="controlClass"
    @update:model-value="handleModelUpdate"
    @change="emit('change', $event)"
    @blur="emit('blur', $event)"
    @focus="emit('focus', $event)"
  />
</template>
