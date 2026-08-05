<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue';

defineOptions({ name: 'DmDialog' });

const props = withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    description?: string;
    closeLabel?: string;
    closeOnBackdrop?: boolean;
    closeOnEscape?: boolean;
    placement?: 'center' | 'right';
    role?: 'dialog' | 'alertdialog';
  }>(),
  {
    description: '',
    closeLabel: '关闭',
    closeOnBackdrop: true,
    closeOnEscape: true,
    placement: 'center',
    role: 'dialog',
  },
);

const emit = defineEmits<{
  close: [];
}>();

const dialogId = useId();
const titleId = `${dialogId}-title`;
const descriptionId = `${dialogId}-description`;
const panelRef = ref<HTMLElement | null>(null);
let previouslyFocusedElement: HTMLElement | null = null;
let previousBodyOverflow = '';

const getFocusableElements = (): HTMLElement[] => {
  const panel = panelRef.value;
  if (panel === null) return [];
  return Array.from(
    panel.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
    ),
  ).filter((element) => !element.hasAttribute('hidden'));
};

const focusDialog = async (): Promise<void> => {
  await nextTick();
  const autofocusElement = panelRef.value?.querySelector<HTMLElement>('[autofocus]');
  (autofocusElement ?? panelRef.value)?.focus();
};

const releaseDialog = (): void => {
  if (typeof document === 'undefined') return;
  document.body.style.overflow = previousBodyOverflow;
  previouslyFocusedElement?.focus();
  previouslyFocusedElement = null;
};

const handleBackdropClick = (): void => {
  if (props.closeOnBackdrop) emit('close');
};

const handleKeydown = (event: KeyboardEvent): void => {
  if (event.key === 'Escape' && props.closeOnEscape) {
    event.preventDefault();
    emit('close');
    return;
  }
  if (event.key !== 'Tab') return;

  const focusableElements = getFocusableElements();
  if (focusableElements.length === 0) {
    event.preventDefault();
    panelRef.value?.focus();
    return;
  }

  const firstElement = focusableElements[0];
  const lastElement = focusableElements[focusableElements.length - 1];
  if (event.shiftKey && document.activeElement === firstElement) {
    event.preventDefault();
    lastElement?.focus();
  } else if (!event.shiftKey && document.activeElement === lastElement) {
    event.preventDefault();
    firstElement?.focus();
  }
};

watch(
  () => props.open,
  (isOpen) => {
    if (typeof document === 'undefined') return;
    if (isOpen) {
      previouslyFocusedElement = document.activeElement as HTMLElement | null;
      previousBodyOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      void focusDialog();
    } else {
      releaseDialog();
    }
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  if (props.open) releaseDialog();
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dm-dialog__backdrop" @mousedown.self="handleBackdropClick">
      <section
        ref="panelRef"
        class="dm-dialog__panel"
        :class="`dm-dialog__panel--${placement}`"
        :role="role"
        aria-modal="true"
        :aria-labelledby="titleId"
        :aria-describedby="description ? descriptionId : undefined"
        tabindex="-1"
        @keydown="handleKeydown"
      >
        <header class="dm-dialog__header">
          <div>
            <h2 :id="titleId">{{ title }}</h2>
            <p v-if="description" :id="descriptionId">{{ description }}</p>
          </div>
          <button type="button" class="dm-dialog__close" @click="emit('close')">
            {{ closeLabel }}
          </button>
        </header>
        <div class="dm-dialog__body"><slot /></div>
        <footer v-if="$slots.footer" class="dm-dialog__footer"><slot name="footer" /></footer>
      </section>
    </div>
  </Teleport>
</template>
