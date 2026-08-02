<script setup lang="ts">
import type { DropdownMenuSeparatorProps } from 'reka-ui';
import { DropdownMenuSeparator } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

const props = defineProps<DropdownMenuSeparatorProps>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <DropdownMenuSeparator
    data-slot="dropdown-menu-separator"
    class="my-1 h-px bg-zinc-200"
    v-bind="forwardedProps"
  />
</template>
