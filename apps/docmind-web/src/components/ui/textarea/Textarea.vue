<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { cn } from '@/lib/utils';

defineOptions({ name: 'ShadcnTextarea' });

const props = defineProps<{
  defaultValue?: string | number;
  modelValue?: string | number;
  class?: HTMLAttributes['class'];
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string | number];
}>();

const modelValue = computed({
  get: () => props.modelValue ?? props.defaultValue ?? '',
  set: (value: string | number) => emit('update:modelValue', value),
});
</script>

<template>
  <textarea v-model="modelValue" data-slot="textarea" :class="cn(props.class)" />
</template>
