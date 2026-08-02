<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type DmButtonVariant =
  | 'primary'
  | 'secondary'
  | 'accent'
  | 'accent-ghost'
  | 'dark'
  | 'ghost'
  | 'danger'
  | 'danger-ghost';
type DmButtonSize = 'small' | 'medium' | 'large';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    variant?: DmButtonVariant;
    size?: DmButtonSize;
    type?: 'button' | 'submit' | 'reset';
    disabled?: boolean;
    loading?: boolean;
    loadingLabel?: string;
    iconOnly?: boolean;
    class?: HTMLAttributes['class'];
  }>(),
  {
    variant: 'primary',
    size: 'medium',
    type: 'button',
    disabled: false,
    loading: false,
    loadingLabel: '处理中',
    iconOnly: false,
  },
);

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();

const buttonClass = computed(() =>
  cn(
    'dm-button !inline-flex h-[30px] min-h-[30px] items-center justify-center gap-1.5 rounded-compact px-3 py-0 text-[12px] leading-none font-medium [&_.app-icon]:flex [&_.app-icon]:size-3.5 [&_.app-icon]:shrink-0 [&_.app-icon]:items-center [&_.app-icon]:justify-center [&_.app-icon]:leading-none [&_.app-icon__svg]:block [&_.app-icon__svg]:size-3.5 [&_.app-icon__svg]:shrink-0',
    `dm-button--${props.variant}`,
    `dm-button--${props.size}`,
    props.iconOnly ? 'w-[30px] px-0' : undefined,
    props.class,
  ),
);

const handleClick = (event: MouseEvent): void => {
  if (props.disabled || props.loading) {
    event.preventDefault();
    return;
  }
  emit('click', event);
};
</script>

<template>
  <Button
    v-bind="$attrs"
    :class="buttonClass"
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
    @click="handleClick"
  >
    <span
      v-if="loading"
      class="dm-button__spinner flex size-3.5 shrink-0 items-center justify-center"
      aria-hidden="true"
    ></span>
    <span
      class="dm-button__label inline-flex items-center justify-center gap-1.5 leading-none"
      :aria-hidden="loading || undefined"
    >
      <slot />
    </span>
    <span v-if="loading" class="dm-sr-only" role="status">{{ loadingLabel }}</span>
  </Button>
</template>
