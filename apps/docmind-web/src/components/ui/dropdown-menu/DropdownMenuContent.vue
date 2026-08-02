<script setup lang="ts">
import type { DropdownMenuContentEmits, DropdownMenuContentProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { computed, useAttrs } from 'vue';
import { DropdownMenuContent, DropdownMenuPortal } from 'reka-ui';

import { withoutKeys, withoutUndefined } from '@/lib/props';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<DropdownMenuContentProps & { class?: HTMLAttributes['class'] }>(),
  {
    align: 'end',
    sideOffset: 6,
  },
);
const emit = defineEmits<DropdownMenuContentEmits>();
const attrs = useAttrs();
const delegatedProps = computed(() => withoutKeys(props, ['class'] as const));
const forwardedProps = computed(() => withoutUndefined(delegatedProps.value));
const forwardedBindings = computed(() => ({ ...forwardedProps.value, ...attrs }));
</script>

<template>
  <DropdownMenuPortal>
    <DropdownMenuContent
      v-bind="forwardedBindings"
      data-slot="dropdown-menu-content"
      :class="
        cn(
          'z-[72] min-w-44 overflow-hidden rounded-md border border-zinc-200 bg-white p-1 text-[12px] text-zinc-700 shadow-float outline-none',
          props.class,
        )
      "
      @escape-key-down="emit('escapeKeyDown', $event)"
      @pointer-down-outside="emit('pointerDownOutside', $event)"
      @focus-outside="emit('focusOutside', $event)"
      @interact-outside="emit('interactOutside', $event)"
      @close-auto-focus="emit('closeAutoFocus', $event)"
    >
      <slot />
    </DropdownMenuContent>
  </DropdownMenuPortal>
</template>
