<script setup lang="ts">
import type { PopoverTriggerProps } from 'reka-ui';
import { PopoverTrigger } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

const props = defineProps<PopoverTriggerProps>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <PopoverTrigger data-slot="popover-trigger" v-bind="forwardedProps">
    <slot />
  </PopoverTrigger>
</template>
