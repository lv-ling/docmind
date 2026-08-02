<script setup lang="ts">
import type { DialogContentEmits, DialogContentProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { computed, useAttrs } from 'vue';
import { DialogContent, DialogOverlay, DialogPortal } from 'reka-ui';

import { withoutKeys, withoutUndefined } from '@/lib/props';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = defineProps<DialogContentProps & { class?: HTMLAttributes['class'] }>();
const emit = defineEmits<DialogContentEmits>();
const attrs = useAttrs();
const delegatedProps = computed(() => withoutKeys(props, ['class'] as const));
const forwardedProps = computed(() => withoutUndefined(delegatedProps.value));
const forwardedBindings = computed(() => ({ ...forwardedProps.value, ...attrs }));
</script>

<template>
  <DialogPortal>
    <DialogOverlay
      data-slot="dialog-overlay"
      class="fixed inset-0 z-[70] bg-zinc-950/35 backdrop-blur-[1px]"
    />
    <DialogContent
      v-bind="forwardedBindings"
      data-slot="dialog-content"
      :class="
        cn(
          'fixed top-1/2 left-1/2 z-[71] grid w-[min(calc(100vw-32px),520px)] -translate-x-1/2 -translate-y-1/2 gap-0 overflow-hidden rounded-lg border border-zinc-200 bg-white text-zinc-900 shadow-float outline-none',
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
    </DialogContent>
  </DialogPortal>
</template>
