<script setup lang="ts">
import { CheckboxIndicator, CheckboxRoot } from 'reka-ui';
import type { HTMLAttributes } from 'vue';

import { cn } from '@/lib/utils';

defineOptions({ name: 'ShadcnCheckbox', inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    modelValue?: boolean | 'indeterminate';
    disabled?: boolean;
    class?: HTMLAttributes['class'];
  }>(),
  {
    modelValue: false,
    disabled: false,
  },
);
const emit = defineEmits<{
  'update:modelValue': [value: boolean | 'indeterminate'];
}>();
</script>

<template>
  <CheckboxRoot
    v-slot="slotProps"
    v-bind="$attrs"
    data-slot="checkbox"
    :model-value="modelValue"
    :disabled="disabled"
    :class="cn('shrink-0 outline-none', props.class)"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <CheckboxIndicator data-slot="checkbox-indicator" class="grid place-content-center">
      <slot v-bind="slotProps" />
    </CheckboxIndicator>
  </CheckboxRoot>
</template>
