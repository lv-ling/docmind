<script setup lang="ts">
import type { DialogTitleProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { computed, useAttrs } from 'vue';
import { DialogTitle } from 'reka-ui';

import { withoutKeys, withoutUndefined } from '@/lib/props';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = defineProps<DialogTitleProps & { class?: HTMLAttributes['class'] }>();
const attrs = useAttrs();
const delegatedProps = computed(() => withoutKeys(props, ['class'] as const));
const forwardedProps = computed(() => withoutUndefined(delegatedProps.value));
const forwardedBindings = computed(() => ({ ...forwardedProps.value, ...attrs }));
</script>

<template>
  <DialogTitle
    v-bind="forwardedBindings"
    data-slot="dialog-title"
    :class="cn('text-[14px] leading-5 font-semibold text-zinc-900', props.class)"
  >
    <slot />
  </DialogTitle>
</template>
