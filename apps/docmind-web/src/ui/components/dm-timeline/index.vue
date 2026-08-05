<script setup lang="ts">
export interface DmTimelineItem {
  id: string;
  title: string;
  description?: string;
  metadata?: string;
  tone?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger';
}

defineOptions({ name: 'DmTimeline' });

withDefaults(
  defineProps<{
    items: DmTimelineItem[];
    ariaLabel?: string;
  }>(),
  {
    ariaLabel: '事件时间线',
  },
);
</script>

<template>
  <ol class="dm-timeline" :aria-label="ariaLabel">
    <li
      v-for="item in items"
      :key="item.id"
      :class="`dm-timeline__item--${item.tone ?? 'neutral'}`"
    >
      <span class="dm-timeline__marker" aria-hidden="true"></span>
      <div class="dm-timeline__copy">
        <div>
          <strong>{{ item.title }}</strong>
          <time v-if="item.metadata">{{ item.metadata }}</time>
        </div>
        <p v-if="item.description">{{ item.description }}</p>
      </div>
    </li>
  </ol>
</template>
