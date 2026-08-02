<script setup lang="ts">
import type { HTMLAttributes } from 'vue';
import { computed } from 'vue';

import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { cn } from '@/lib/utils';

import type { DmTabItem } from './types.js';

defineOptions({ inheritAttrs: false });

const props = defineProps<{
  modelValue: string;
  items: readonly DmTabItem[];
  label: string;
  class?: HTMLAttributes['class'];
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
  change: [value: string];
}>();

const rootClass = computed(() => cn('min-w-0', props.class));

const handleModelUpdate = (value: string | number): void => {
  if (typeof value !== 'string' || value === props.modelValue) return;
  emit('update:modelValue', value);
  emit('change', value);
};
</script>

<template>
  <Tabs
    v-bind="$attrs"
    :model-value="modelValue"
    activation-mode="automatic"
    orientation="horizontal"
    :class="rootClass"
    @update:model-value="handleModelUpdate"
  >
    <TabsList
      class="dm-tabs w-full [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      :aria-label="label"
    >
      <TabsTrigger
        v-for="item in items"
        :key="item.value"
        :value="item.value"
        class="dm-tabs__tab"
        :class="{ 'dm-tabs__tab--active': item.value === modelValue }"
        :aria-controls="item.panelId ?? undefined"
        :data-tab-value="item.value"
        :disabled="Boolean(item.disabled)"
      >
        <span>{{ item.label }}</span>
        <span v-if="item.count !== undefined" class="dm-tabs__count">{{ item.count }}</span>
      </TabsTrigger>
    </TabsList>
  </Tabs>
</template>
