<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed, useSlots } from 'vue';

import AppIcon from '@/components/AppIcon.vue';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { cn } from '@/lib/utils';

import DmButton from './DmButton.vue';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    title: string;
    description?: string;
    confirmLabel?: string;
    cancelLabel?: string;
    loading?: boolean;
    confirmDisabled?: boolean;
    showFooter?: boolean;
    closeLabel?: string;
    class?: HTMLAttributes['class'];
  }>(),
  {
    description: '',
    confirmLabel: '确认',
    cancelLabel: '取消',
    loading: false,
    confirmDisabled: false,
    showFooter: true,
    closeLabel: '关闭弹窗',
  },
);

const emit = defineEmits<{
  confirm: [];
  cancel: [];
}>();

const modelValue = defineModel<boolean>({ required: true });
const slots = useSlots();
const contentClass = computed(() => cn('dm-dialog', props.class));

const handleOpenUpdate = (open: boolean): void => {
  if (props.loading && !open) return;
  modelValue.value = open;
};

const handleCancel = (): void => {
  if (props.loading) return;
  emit('cancel');
  modelValue.value = false;
};

const handleConfirm = (): void => {
  if (props.loading || props.confirmDisabled) return;
  emit('confirm');
};

const handleDismissAttempt = (event: Event): void => {
  if (props.loading) event.preventDefault();
};
</script>

<template>
  <Dialog :open="modelValue" @update:open="handleOpenUpdate">
    <DialogTrigger v-if="slots.trigger" as-child>
      <slot name="trigger" />
    </DialogTrigger>

    <DialogContent
      v-bind="$attrs"
      :class="contentClass"
      @escape-key-down="handleDismissAttempt"
      @pointer-down-outside="handleDismissAttempt"
    >
      <header class="relative border-b border-zinc-200 px-5 py-4 pr-14">
        <DialogTitle>{{ title }}</DialogTitle>
        <DialogDescription v-if="description" class="mt-1">{{ description }}</DialogDescription>
        <DmButton
          variant="ghost"
          icon-only
          class="absolute top-3.5 right-4"
          :disabled="loading"
          :aria-label="closeLabel"
          @click="handleCancel"
        >
          <AppIcon name="close" />
        </DmButton>
      </header>

      <div class="min-h-0 px-5 py-4 text-[12px] leading-5 text-zinc-700">
        <slot />
      </div>

      <footer
        v-if="showFooter"
        class="flex items-center justify-end gap-2 border-t border-zinc-200 bg-zinc-50 px-5 py-3"
      >
        <slot name="footer" :cancel="handleCancel" :confirm="handleConfirm" :loading="loading">
          <DmButton variant="secondary" :disabled="loading" @click="handleCancel">
            {{ cancelLabel }}
          </DmButton>
          <DmButton
            :loading="loading"
            :loading-label="`${confirmLabel}中`"
            :disabled="confirmDisabled"
            @click="handleConfirm"
          >
            {{ confirmLabel }}
          </DmButton>
        </slot>
      </footer>
    </DialogContent>
  </Dialog>
</template>
