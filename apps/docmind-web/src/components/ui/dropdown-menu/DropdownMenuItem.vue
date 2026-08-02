<script setup lang="ts">
import type { DropdownMenuItemEmits, DropdownMenuItemProps } from 'reka-ui';
import type { HTMLAttributes } from 'vue';
import { computed, useAttrs } from 'vue';
import { DropdownMenuItem } from 'reka-ui';

import { withoutKeys, withoutUndefined } from '@/lib/props';
import { cn } from '@/lib/utils';

defineOptions({ inheritAttrs: false });

const props = defineProps<DropdownMenuItemProps & { class?: HTMLAttributes['class'] }>();
const emit = defineEmits<DropdownMenuItemEmits>();
const attrs = useAttrs();
const delegatedProps = computed(() => withoutKeys(props, ['class'] as const));
const forwardedProps = computed(() => withoutUndefined(delegatedProps.value));
const forwardedBindings = computed(() => ({ ...forwardedProps.value, ...attrs }));
</script>

<template>
  <DropdownMenuItem
    v-bind="forwardedBindings"
    data-slot="dropdown-menu-item"
    :class="
      cn(
        'relative flex min-h-[30px] cursor-default select-none items-center gap-2 rounded-compact px-2.5 py-1.5 outline-none transition-colors focus:bg-zinc-100 focus:text-zinc-900 data-[disabled]:pointer-events-none data-[disabled]:opacity-45',
        props.class,
      )
    "
    @select="emit('select', $event)"
  >
    <slot />
  </DropdownMenuItem>
</template>
