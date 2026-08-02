<script setup lang="ts">
import type { DialogDescriptionProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { computed, useAttrs } from 'vue';
import { DialogDescription } from 'reka-ui';

import { withoutKeys, withoutUndefined } from '@/lib/props';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = defineProps<DialogDescriptionProps & { class?: HTMLAttributes['class'] }>();
const attrs = useAttrs();
const delegatedProps = computed(() => withoutKeys(props, ['class'] as const));
const forwardedProps = computed(() => withoutUndefined(delegatedProps.value));
const forwardedBindings = computed(() => ({ ...forwardedProps.value, ...attrs }));
</script>

<template>
  <DialogDescription
    v-bind="forwardedBindings"
    data-slot="dialog-description"
    :class="cn('text-[12px] leading-5 text-zinc-500', props.class)"
  >
    <slot />
  </DialogDescription>
</template>
