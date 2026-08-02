<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = defineProps<{
  modelValue?: string;
  class?: HTMLAttributes['class'];
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const modelValue = computed({
  get: () => props.modelValue ?? '',
  set: (value: string) => emit('update:modelValue', value),
});
</script>

<template>
  <select v-bind="$attrs" v-model="modelValue" data-slot="native-select" :class="cn(props.class)">
    <slot />
  </select>
</template>
