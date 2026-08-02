<script setup lang="ts">
import type { DialogRootEmits, DialogRootProps } from 'reka-ui';
import { DialogRoot } from 'reka-ui';
import { computed } from 'vue';

import { withoutUndefined } from '@/lib/props';

defineOptions({ name: 'ShadcnDialog' });

const props = defineProps<DialogRootProps>();
const emit = defineEmits<DialogRootEmits>();
const forwardedProps = computed(() => withoutUndefined(props));
</script>

<template>
  <DialogRoot data-slot="dialog" v-bind="forwardedProps" @update:open="emit('update:open', $event)">
    <slot />
  </DialogRoot>
</template>
