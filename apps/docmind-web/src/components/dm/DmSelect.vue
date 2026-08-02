<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { NativeSelect } from '@/components/ui/native-select';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    id: string;
    disabled?: boolean;
    class?: HTMLAttributes['class'];
  }>(),
  {
    disabled: false,
  },
);

const modelValue = defineModel<string>({ required: true });
const controlClass = computed(() =>
  cn(
    'dm-control h-8 min-w-0 rounded-compact border border-zinc-200 bg-white px-2.5 text-[12px] leading-none text-zinc-700 outline-none transition-colors duration-interaction focus:border-brand-400 focus:ring-2 focus:ring-brand-100/50 disabled:cursor-not-allowed disabled:bg-zinc-100 disabled:text-zinc-400',
    props.class,
  ),
);
</script>

<template>
  <NativeSelect
    v-bind="$attrs"
    :id="id"
    v-model="modelValue"
    :disabled="disabled"
    :class="controlClass"
  >
    <slot />
  </NativeSelect>
</template>
