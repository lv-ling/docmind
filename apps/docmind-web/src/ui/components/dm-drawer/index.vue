<script setup lang="ts">
import DmDialog from '../dm-dialog/index.vue';

defineOptions({ name: 'DmDrawer' });

withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    description?: string;
    closeLabel?: string;
    closeOnBackdrop?: boolean;
    closeOnEscape?: boolean;
  }>(),
  {
    description: '',
    closeLabel: '关闭',
    closeOnBackdrop: true,
    closeOnEscape: true,
  },
);

const emit = defineEmits<{
  close: [];
}>();
</script>

<template>
  <DmDialog
    :open="open"
    :title="title"
    :description="description"
    :close-label="closeLabel"
    :close-on-backdrop="closeOnBackdrop"
    :close-on-escape="closeOnEscape"
    placement="right"
    @close="emit('close')"
  >
    <slot />
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </DmDialog>
</template>
