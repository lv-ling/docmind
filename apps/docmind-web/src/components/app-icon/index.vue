<script setup lang="ts">
import { computed } from 'vue';

defineOptions({ name: 'AppIcon' });

const props = defineProps<{
  name:
    | 'activity'
    | 'alert'
    | 'arrow'
    | 'bell'
    | 'bot'
    | 'chevrons'
    | 'close'
    | 'cpu'
    | 'document'
    | 'documents'
    | 'filter'
    | 'grid'
    | 'lightbulb'
    | 'logout'
    | 'plus'
    | 'refresh'
    | 'review'
    | 'schema'
    | 'search'
    | 'settings'
    | 'sparkles'
    | 'template'
    | 'upload';
  label?: string;
}>();

const paths: Record<typeof props.name, string[]> = {
  activity: ['M3 12h4l2-7 4 14 2-7h6'],
  alert: ['M12 3 2.5 20h19z', 'M12 9v4', 'M12 17h.01'],
  bell: ['M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9', 'M10 21h4'],
  bot: ['M8 4h8', 'M12 2v2', 'M5 8h14v11H5z', 'M9 12h.01', 'M15 12h.01', 'M9 16h6'],
  chevrons: ['m7 9 5-5 5 5', 'm7 15 5 5 5-5'],
  close: ['m6 6 12 12', 'M18 6 6 18'],
  cpu: [
    'M8 8h8v8H8z',
    'M3 9h2',
    'M3 15h2',
    'M19 9h2',
    'M19 15h2',
    'M9 3v2',
    'M15 3v2',
    'M9 19v2',
    'M15 19v2',
  ],
  document: ['M6 2h8l4 4v16H6z', 'M14 2v5h5', 'M9 12h6', 'M9 16h6'],
  documents: ['M8 3h9l4 4v13H8z', 'M17 3v5h5', 'M4 7v14h10'],
  filter: ['M4 5h16l-6 7v5l-4 2v-7z'],
  grid: ['M4 4h6v6H4z', 'M14 4h6v6h-6z', 'M4 14h6v6H4z', 'M14 14h6v6h-6z'],
  lightbulb: ['M9 18h6', 'M10 22h4', 'M8.5 15.5A7 7 0 1 1 15.5 15.5L14 18h-4z'],
  plus: ['M12 5v14', 'M5 12h14'],
  refresh: [
    'M20 7v5h-5',
    'M4 17v-5h5',
    'M6.1 9a7 7 0 0 1 11.8-2L20 10',
    'M4 14l2.1 3A7 7 0 0 0 18 15',
  ],
  schema: ['M4 4h6v6H4z', 'M14 4h6v6h-6z', 'M9 14h6v6H9z', 'M7 10v2h10v-2', 'M12 12v2'],
  search: ['M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16', 'm21 21-4.35-4.35'],
  settings: [
    'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7',
    'M19.4 15a1.8 1.8 0 0 0 .36 1.98l.06.06-2.12 2.12-.06-.06a1.8 1.8 0 0 0-1.98-.36 1.8 1.8 0 0 0-1.06 1.65V20h-3v-.09a1.8 1.8 0 0 0-1.06-1.65 1.8 1.8 0 0 0-1.98.36l-.06.06-2.12-2.12.06-.06A1.8 1.8 0 0 0 6.6 15a1.8 1.8 0 0 0-1.65-1.06H5v-3h.09A1.8 1.8 0 0 0 6.74 9.9a1.8 1.8 0 0 0-.36-1.98l-.06-.06L8.44 5.74l.06.06a1.8 1.8 0 0 0 1.98.36A1.8 1.8 0 0 0 11.54 4.5V4h3v.5a1.8 1.8 0 0 0 1.06 1.65 1.8 1.8 0 0 0 1.98-.36l.06-.06 2.12 2.12-.06.06a1.8 1.8 0 0 0-.36 1.98A1.8 1.8 0 0 0 21 10.95H21v3h-.09A1.8 1.8 0 0 0 19.4 15z',
  ],
  sparkles: [
    'm12 3 1.2 3.1L16 7.3l-2.8 1.2L12 12l-1.2-3.5L8 7.3l2.8-1.2z',
    'm18 14 .7 1.8 1.8.7-1.8.7L18 19l-.7-1.8-1.8-.7 1.8-.7z',
    'm5 13 .8 2.2L8 16l-2.2.8L5 19l-.8-2.2L2 16l2.2-.8z',
  ],
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
