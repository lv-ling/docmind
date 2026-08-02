<script setup lang="ts">
import type { DropdownMenuTriggerProps } from 'reka-ui';
import { DropdownMenuTrigger } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

const props = defineProps<DropdownMenuTriggerProps>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <DropdownMenuTrigger data-slot="dropdown-menu-trigger" v-bind="forwardedProps">
    <slot />
  </DropdownMenuTrigger>
</template>
