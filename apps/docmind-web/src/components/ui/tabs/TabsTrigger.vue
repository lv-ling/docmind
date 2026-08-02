<script setup lang="ts">
import type { TabsTriggerProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { computed, useAttrs } from 'vue';
import { TabsTrigger } from 'reka-ui';

import { withoutKeys, withoutUndefined } from '@/lib/props';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = defineProps<TabsTriggerProps & { class?: HTMLAttributes['class'] }>();
const attrs = useAttrs();
const delegatedProps = computed(() => withoutKeys(props, ['class', 'value'] as const));
const forwardedProps = computed(() => withoutUndefined(delegatedProps.value));
const forwardedBindings = computed(() => ({ ...forwardedProps.value, ...attrs }));
</script>

<template>
  <TabsTrigger
    v-bind="forwardedBindings"
    :value="props.value"
    data-slot="tabs-trigger"
    :class="cn('inline-flex shrink-0 items-center justify-center outline-none', props.class)"
  >
    <slot />
  </TabsTrigger>
</template>
