<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    align?: 'start' | 'center' | 'end';
    side?: 'top' | 'right' | 'bottom' | 'left';
    sideOffset?: number;
    modal?: boolean;
    contentClass?: HTMLAttributes['class'];
  }>(),
  {
    align: 'center',
    side: 'bottom',
    sideOffset: 6,
    modal: false,
  },
);

const modelValue = defineModel<boolean>({ default: false });
const popoverClass = computed(() => cn('dm-popover', props.contentClass));
</script>

<template>
  <Popover v-model:open="modelValue" :modal="modal">
    <PopoverTrigger as-child>
      <slot name="trigger" />
    </PopoverTrigger>
    <PopoverContent
      v-bind="$attrs"
      :align="align"
      :side="side"
      :side-offset="sideOffset"
      :class="popoverClass"
    >
      <slot />
    </PopoverContent>
  </Popover>
</template>
