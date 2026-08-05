<script setup lang="ts">
import { DmButton, DmEmptyState } from '@/ui';

import DocumentSourceBadge, { type DocumentSourceType } from '../document-source-badge/index.vue';
import DocumentStatusBadge from '../document-status-badge/index.vue';

export interface DocumentPickerItem {
  id: string;
  name: string;
  sourceType: DocumentSourceType;
  subtitle?: string;
  status?: string;
  disabled?: boolean;
  disabledReason?: string;
}

defineOptions({ name: 'DocumentPicker' });

withDefaults(
  defineProps<{
    items: DocumentPickerItem[];
    label?: string;
    description?: string;
    emptyTitle?: string;
    emptyDescription?: string;
    allowUpload?: boolean;
    uploadLabel?: string;
  }>(),
  {
    label: '选择文档',
    description: '',
    emptyTitle: '没有可选择的文档',
    emptyDescription: '请调整筛选条件，或上传一份新文档。',
    allowUpload: false,
    uploadLabel: '上传新文档',
  },
);

const selectedId = defineModel<string | null>({ default: null });
const emit = defineEmits<{
  upload: [];
}>();

const selectItem = (item: DocumentPickerItem): void => {
  if (item.disabled) return;
  selectedId.value = item.id;
};
</script>

<template>
  <section class="document-picker" :aria-labelledby="`${$attrs.id ?? 'document-picker'}-label`">
    <header class="document-picker__header">
      <div>
        <h2 :id="`${$attrs.id ?? 'document-picker'}-label`">{{ label }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <DmButton v-if="allowUpload" size="small" variant="secondary" @click="emit('upload')">
        {{ uploadLabel }}
      </DmButton>
    </header>

    <div
      v-if="items.length > 0"
      class="document-picker__list"
      role="radiogroup"
      :aria-label="label"
    >
      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        role="radio"
        class="document-picker__item"
        :class="{ 'document-picker__item--selected': selectedId === item.id }"
        :aria-checked="selectedId === item.id"
        :disabled="item.disabled"
        @click="selectItem(item)"
      >
        <span class="document-picker__item-copy">
          <strong>{{ item.name }}</strong>
          <small v-if="item.subtitle">{{ item.subtitle }}</small>
          <small v-if="item.disabledReason" class="document-picker__reason">
            {{ item.disabledReason }}
          </small>
        </span>
        <span class="document-picker__badges">
          <DocumentSourceBadge :type="item.sourceType" />
          <DocumentStatusBadge v-if="item.status" :status="item.status" />
        </span>
      </button>
    </div>

    <DmEmptyState v-else :title="emptyTitle" :description="emptyDescription">
      <template v-if="allowUpload" #actions>
        <DmButton size="small" @click="emit('upload')">{{ uploadLabel }}</DmButton>
      </template>
    </DmEmptyState>
  </section>
</template>

<style scoped>
.document-picker {
  overflow: hidden;
  border: 1px solid var(--dm-color-border);
  border-radius: var(--dm-radius-large);
  background: var(--dm-color-paper);
  box-shadow: var(--dm-shadow-card);
}

.document-picker__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-bottom: 1px solid var(--dm-color-border);
}

.document-picker__header h2,
.document-picker__header p {
  margin: 0;
}

.document-picker__header h2 {
  color: var(--dm-color-ink);
  font-size: 0.875rem;
}

.document-picker__header p {
  margin-top: 0.25rem;
  color: var(--dm-color-ink-muted);
  font-size: 0.6875rem;
  line-height: 1.5;
}

.document-picker__list {
  display: grid;
  gap: 0.5rem;
  padding: 0.75rem;
}

.document-picker__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
  padding: 0.8rem;
  border: 1px solid var(--dm-color-border);
  border-radius: var(--dm-radius-control);
  color: var(--dm-color-ink-secondary);
  background: var(--dm-color-paper);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--dm-motion-fast) var(--dm-motion-easing),
    background-color var(--dm-motion-fast) var(--dm-motion-easing),
    box-shadow var(--dm-motion-fast) var(--dm-motion-easing);
}

.document-picker__item:hover:not(:disabled) {
  border-color: var(--dm-color-border-strong);
  background: var(--dm-color-paper-muted);
}

.document-picker__item--selected {
  border-color: var(--dm-color-brand);
  background: var(--dm-color-brand-soft);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--dm-color-brand) 10%, transparent);
}

.document-picker__item:focus-visible {
  outline: 2px solid var(--dm-color-brand);
  outline-offset: 2px;
}

.document-picker__item:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.document-picker__item-copy {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.document-picker__item-copy strong,
.document-picker__item-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-picker__item-copy strong {
  color: var(--dm-color-ink);
  font-size: 0.75rem;
}

.document-picker__item-copy small {
  color: var(--dm-color-ink-muted);
  font-size: 0.625rem;
}

.document-picker__reason {
  color: var(--dm-color-danger) !important;
}

.document-picker__badges {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.35rem;
}

@media (max-width: 36rem) {
  .document-picker__header,
  .document-picker__item {
    align-items: flex-start;
    flex-direction: column;
  }

  .document-picker__badges {
    justify-content: flex-start;
  }
}
</style>
