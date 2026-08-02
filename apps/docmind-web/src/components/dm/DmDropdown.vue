<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';

import type { DmDropdownEntry, DmDropdownItem } from './types.js';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    items: readonly DmDropdownEntry[];
    align?: 'start' | 'center' | 'end';
    side?: 'top' | 'right' | 'bottom' | 'left';
    sideOffset?: number;
    modal?: boolean;
    contentClass?: HTMLAttributes['class'];
  }>(),
  {
    align: 'end',
    side: 'bottom',
    sideOffset: 6,
    modal: true,
  },
);

const emit = defineEmits<{
  select: [value: string, item: DmDropdownItem];
}>();

const modelValue = defineModel<boolean>({ default: false });
const menuClass = computed(() => cn('dm-dropdown', props.contentClass));

const handleSelect = (item: DmDropdownItem): void => {
  if (item.disabled) return;
  emit('select', item.value, item);
};
</script>

<template>
  <DropdownMenu v-model:open="modelValue" :modal="modal">
    <DropdownMenuTrigger as-child>
      <slot name="trigger" />
    </DropdownMenuTrigger>
    <DropdownMenuContent
      v-bind="$attrs"
      :align="align"
      :side="side"
      :side-offset="sideOffset"
      :class="menuClass"
    >
      <template v-for="entry in items" :key="entry.type === 'item' ? entry.value : entry.key">
        <DropdownMenuSeparator v-if="entry.type === 'separator'" />
        <DropdownMenuItem
          v-else
          :disabled="Boolean(entry.disabled)"
          :class="entry.destructive ? 'text-danger focus:bg-danger-soft focus:text-danger' : ''"
          @select="handleSelect(entry)"
        >
          <slot name="item" :item="entry">
            {{ entry.label }}
          </slot>
        </DropdownMenuItem>
      </template>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
