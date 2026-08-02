<script setup lang="ts">
type DmButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
type DmButtonSize = 'small' | 'medium' | 'large';

const props = withDefaults(
  defineProps<{
    variant?: DmButtonVariant;
    size?: DmButtonSize;
    type?: 'button' | 'submit' | 'reset';
    disabled?: boolean;
    loading?: boolean;
    loadingLabel?: string;
  }>(),
  {
    variant: 'primary',
    size: 'medium',
    type: 'button',
    disabled: false,
    loading: false,
    loadingLabel: '处理中',
  },
);

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();

const handleClick = (event: MouseEvent): void => {
  if (props.disabled || props.loading) {
    event.preventDefault();
    return;
  }
  emit('click', event);
};
</script>

<template>
  <button
    :class="[
      'dm-button inline-flex h-[30px] min-h-[30px] items-center justify-center gap-1.5 rounded-compact px-3 py-0 text-[12px] leading-none font-medium [&_.app-icon]:size-3.5 [&_.dm-button__label]:inline-flex [&_.dm-button__label]:items-center [&_.dm-button__label]:gap-1.5 [&_.dm-button__label]:leading-none',
      `dm-button--${variant}`,
      `dm-button--${size}`,
    ]"
    :type="type"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
    @click="handleClick"
  >
    <span v-if="loading" class="dm-button__spinner" aria-hidden="true"></span>
    <span class="dm-button__label" :aria-hidden="loading || undefined"><slot /></span>
    <span v-if="loading" class="dm-sr-only" role="status">{{ loadingLabel }}</span>
  </button>
</template>
