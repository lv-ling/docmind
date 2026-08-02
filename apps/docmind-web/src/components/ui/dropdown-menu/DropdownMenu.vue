<script setup lang="ts">
import type { DropdownMenuRootEmits, DropdownMenuRootProps } from 'reka-ui';
import { DropdownMenuRoot } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

const props = defineProps<DropdownMenuRootProps>();
const emit = defineEmits<DropdownMenuRootEmits>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <DropdownMenuRoot
    data-slot="dropdown-menu"
    v-bind="forwardedProps"
    @update:open="emit('update:open', $event)"
  >
    <slot />
  </DropdownMenuRoot>
</template>
