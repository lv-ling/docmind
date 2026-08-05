<script setup lang="ts">
import { computed, useId } from 'vue';

export interface DmTabItem {
  label: string;
  value: string;
  count?: number;
  disabled?: boolean;
}

defineOptions({ name: 'DmTabs' });

const props = defineProps<{
  items: DmTabItem[];
  ariaLabel: string;
}>();

const modelValue = defineModel<string>({ required: true });
const componentId = useId();
const selectedIndex = computed(() =>
  Math.max(
    0,
    props.items.findIndex((item) => item.value === modelValue.value),
  ),
);

const selectRelativeTab = (direction: 1 | -1): void => {
  if (props.items.length === 0) return;
  let nextIndex = selectedIndex.value;
  for (let step = 0; step < props.items.length; step += 1) {
    nextIndex = (nextIndex + direction + props.items.length) % props.items.length;
    const item = props.items[nextIndex];
    if (item !== undefined && item.disabled !== true) {
      modelValue.value = item.value;
      break;
    }
  }
};

const handleKeydown = (event: KeyboardEvent): void => {
  if (event.key === 'ArrowRight') selectRelativeTab(1);
  else if (event.key === 'ArrowLeft') selectRelativeTab(-1);
  else return;
  event.preventDefault();
};
</script>

<template>
  <div class="dm-tabs">
    <div class="dm-tabs__list" role="tablist" :aria-label="ariaLabel" @keydown="handleKeydown">
      <button
        v-for="item in items"
        :id="`${componentId}-${item.value}-tab`"
        :key="item.value"
        type="button"
        role="tab"
        :aria-selected="modelValue === item.value"
        :aria-controls="`${componentId}-${item.value}-panel`"
        :tabindex="modelValue === item.value ? 0 : -1"
        :disabled="item.disabled"
        @click="modelValue = item.value"
      >
        <span>{{ item.label }}</span>
        <span v-if="item.count !== undefined" class="dm-tabs__count">{{ item.count }}</span>
      </button>
    </div>
    <section
      :id="`${componentId}-${modelValue}-panel`"
      class="dm-tabs__panel"
      role="tabpanel"
      :aria-labelledby="`${componentId}-${modelValue}-tab`"
    >
      <slot :value="modelValue" />
    </section>
  </div>
</template>
