<script setup lang="ts">
import DmButton from '../dm-button/index.vue';
import DmDialog from '../dm-dialog/index.vue';

defineOptions({ name: 'DmAlertDialog' });

withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    description: string;
    confirmLabel?: string;
    cancelLabel?: string;
    confirmTone?: 'primary' | 'danger';
    loading?: boolean;
  }>(),
  {
    confirmLabel: '确认',
    cancelLabel: '取消',
    confirmTone: 'primary',
    loading: false,
  },
);

const emit = defineEmits<{
  close: [];
  confirm: [];
}>();
</script>

<template>
  <DmDialog
    :open="open"
    :title="title"
    :description="description"
    role="alertdialog"
    :close-on-backdrop="!loading"
    :close-on-escape="!loading"
    @close="emit('close')"
  >
    <slot />
    <template #footer>
      <DmButton variant="secondary" :disabled="loading" @click="emit('close')">
        {{ cancelLabel }}
      </DmButton>
      <DmButton
        :variant="confirmTone === 'danger' ? 'danger' : 'primary'"
        :loading="loading"
        @click="emit('confirm')"
      >
        {{ confirmLabel }}
      </DmButton>
    </template>
  </DmDialog>
</template>
