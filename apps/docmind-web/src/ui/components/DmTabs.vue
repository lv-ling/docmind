<script setup lang="ts">
import { useId } from 'vue';

interface DmTabItem {
  value: string;
  label: string;
  count?: number;
  disabled?: boolean;
  panelId?: string;
}

const props = defineProps<{
  modelValue: string;
  items: readonly DmTabItem[];
  label: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
  change: [value: string];
}>();

const componentId = useId();

const selectTab = (item: DmTabItem): void => {
  if (item.disabled || item.value === props.modelValue) return;
  emit('update:modelValue', item.value);
  emit('change', item.value);
};

const handleKeydown = (event: KeyboardEvent, item: DmTabItem): void => {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;

  const enabledItems = props.items.filter((candidate) => !candidate.disabled);
  const currentIndex = enabledItems.findIndex((candidate) => candidate.value === item.value);
  if (currentIndex < 0 || enabledItems.length === 0) return;

  let nextIndex: number;
  if (event.key === 'Home') nextIndex = 0;
  else if (event.key === 'End') nextIndex = enabledItems.length - 1;
  else if (event.key === 'ArrowLeft') {
    nextIndex = (currentIndex - 1 + enabledItems.length) % enabledItems.length;
  } else {
    nextIndex = (currentIndex + 1) % enabledItems.length;
  }

  event.preventDefault();
  const nextItem = enabledItems[nextIndex];
  if (nextItem === undefined) return;
  selectTab(nextItem);

  const tabList = (event.currentTarget as HTMLElement).closest('[role="tablist"]');
  const nextTab = tabList?.querySelector<HTMLElement>(
    `[data-tab-value="${CSS.escape(nextItem.value)}"]`,
  );
  nextTab?.focus();
};
</script>

<template>
  <div class="dm-tabs" role="tablist" :aria-label="label">
    <button
      v-for="item in items"
      :id="`${componentId}-${item.value}-tab`"
      :key="item.value"
      type="button"
      role="tab"
      class="dm-tabs__tab"
      :class="{ 'dm-tabs__tab--active': item.value === modelValue }"
      :aria-selected="item.value === modelValue"
      :aria-controls="item.panelId"
      :data-tab-value="item.value"
      :disabled="item.disabled"
      :tabindex="item.value === modelValue ? 0 : -1"
      @click="selectTab(item)"
      @keydown="handleKeydown($event, item)"
    >
      <span>{{ item.label }}</span>
      <span v-if="item.count !== undefined" class="dm-tabs__count">{{ item.count }}</span>
    </button>
  </div>
</template>
