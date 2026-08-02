<script setup lang="ts">
import type { PopoverContentEmits, PopoverContentProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { computed, useAttrs } from 'vue';
import { PopoverContent, PopoverPortal } from 'reka-ui';

import { withoutKeys, withoutUndefined } from '@/lib/props';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<PopoverContentProps & { class?: HTMLAttributes['class'] }>(),
  {
    align: 'center',
    sideOffset: 6,
  },
);
const emit = defineEmits<PopoverContentEmits>();
const attrs = useAttrs();
const delegatedProps = computed(() => withoutKeys(props, ['class'] as const));
const forwardedProps = computed(() => withoutUndefined(delegatedProps.value));
const forwardedBindings = computed(() => ({ ...forwardedProps.value, ...attrs }));
</script>

<template>
  <PopoverPortal>
    <PopoverContent
      v-bind="forwardedBindings"
      data-slot="popover-content"
      :class="
        cn(
          'z-[72] w-72 rounded-md border border-zinc-200 bg-white p-4 text-[12px] leading-5 text-zinc-700 shadow-float outline-none',
          props.class,
        )
      "
      @escape-key-down="emit('escapeKeyDown', $event)"
      @pointer-down-outside="emit('pointerDownOutside', $event)"
      @focus-outside="emit('focusOutside', $event)"
      @interact-outside="emit('interactOutside', $event)"
      @open-auto-focus="emit('openAutoFocus', $event)"
      @close-auto-focus="emit('closeAutoFocus', $event)"
    >
      <slot />
    </PopoverContent>
  </PopoverPortal>
</template>
