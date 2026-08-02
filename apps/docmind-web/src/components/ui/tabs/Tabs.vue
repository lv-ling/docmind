<script setup lang="ts">
import type { TabsRootEmits, TabsRootProps } from 'reka-ui';
import { TabsRoot } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

defineOptions({ name: 'ShadcnTabs' });

const props = defineProps<TabsRootProps>();
const emit = defineEmits<TabsRootEmits>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <TabsRoot
    data-slot="tabs"
    v-bind="forwardedProps"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <slot />
  </TabsRoot>
</template>
