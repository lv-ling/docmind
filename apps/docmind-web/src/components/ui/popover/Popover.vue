<script setup lang="ts">
import type { PopoverRootEmits, PopoverRootProps } from 'reka-ui';
import { PopoverRoot } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

defineOptions({ name: 'ShadcnPopover' });

const props = defineProps<PopoverRootProps>();
const emit = defineEmits<PopoverRootEmits>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <PopoverRoot
    data-slot="popover"
    v-bind="forwardedProps"
    @update:open="emit('update:open', $event)"
  >
    <slot />
  </PopoverRoot>
</template>
