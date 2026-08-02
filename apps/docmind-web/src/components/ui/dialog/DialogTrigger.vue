<script setup lang="ts">
import type { DialogTriggerProps } from 'reka-ui';
import { DialogTrigger } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

const props = defineProps<DialogTriggerProps>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <DialogTrigger data-slot="dialog-trigger" v-bind="forwardedProps">
    <slot />
  </DialogTrigger>
</template>
