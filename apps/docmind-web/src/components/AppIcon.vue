<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  name: 'document' | 'schema' | 'review' | 'template' | 'upload' | 'arrow' | 'logout';
  label?: string;
}>();

const paths: Record<typeof props.name, string[]> = {
  document: ['M6 2h8l4 4v16H6z', 'M14 2v5h5', 'M9 12h6', 'M9 16h6'],
  schema: ['M4 4h6v6H4z', 'M14 4h6v6h-6z', 'M9 14h6v6H9z', 'M7 10v2h10v-2', 'M12 12v2'],
  review: ['M3 5h18v14H3z', 'M8 9h8', 'M8 13h5', 'm15 16 2 2 4-5'],
  template: ['M5 3h14v18H5z', 'M9 3v18', 'M12 8h4', 'M12 12h4'],
  upload: ['M12 16V4', 'm7 9 5-5 5 5', 'M5 20h14'],
  arrow: ['m9 5 7 7-7 7'],
  logout: ['M10 5H4v14h6', 'M14 8l4 4-4 4', 'M18 12H8'],
};

const ariaHidden = computed(() => (props.label === undefined ? 'true' : undefined));
</script>

<template>
  <svg
    class="app-icon"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    :aria-hidden="ariaHidden"
    :aria-label="label"
    :role="label === undefined ? undefined : 'img'"
  >
    <path v-for="path in paths[name]" :key="path" :d="path" />
  </svg>
</template>
