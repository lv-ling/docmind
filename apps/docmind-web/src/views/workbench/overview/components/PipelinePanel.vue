<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';

import type { WorkbenchPipelineItem } from '../model/workbench-overview.js';

defineProps<{ items: WorkbenchPipelineItem[] }>();

const emit = defineEmits<{
  'open-item': [item: WorkbenchPipelineItem];
}>();
</script>

<template>
  <section id="workbench-pipeline" class="pipeline-panel" aria-labelledby="pipeline-title">
    <header>
      <h2 id="pipeline-title">处理管线</h2>
    </header>

    <div class="pipeline-panel__list">
      <button v-for="item in items" :key="item.id" type="button" @click="emit('open-item', item)">
        <span class="pipeline-panel__engine" aria-hidden="true"><AppIcon name="cpu" /></span>
        <span class="pipeline-panel__copy">
          <strong>{{ item.title }}</strong>
          <small>包含 {{ item.documentCount }} 份文件</small>
        </span>
        <span class="pipeline-panel__status">
          <span><AppIcon name="activity" />{{ item.stageLabel }}</span>
          <strong>{{ item.progress }}%</strong>
        </span>
        <span class="pipeline-panel__progress" aria-hidden="true">
          <i :style="{ width: `${item.progress}%` }"></i>
        </span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.pipeline-panel {
  display: grid;
  gap: 11px;
  scroll-margin-top: 72px;
}

.pipeline-panel h2 {
  margin: 0;
  color: var(--dm-color-zinc-800);
  font-size: 13px;
  font-weight: 700;
}

.pipeline-panel__list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.pipeline-panel__list > button {
  display: grid;
  grid-template-columns: 31px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  padding: 12px;
  text-align: left;
  border: 1px solid #c7d2fe;
  border-radius: var(--dm-radius-medium);
  background: rgb(238 242 255 / 26%);
  box-shadow: var(--dm-shadow-card);
  cursor: pointer;
  transition:
    background var(--dm-motion-fast) var(--dm-motion-easing),
    transform var(--dm-motion-fast) var(--dm-motion-easing),
    box-shadow var(--dm-motion-fast) var(--dm-motion-easing);
}

.pipeline-panel__list > button:hover {
  background: rgb(238 242 255 / 54%);
  box-shadow: 0 5px 16px rgb(99 102 241 / 7%);
  transform: translateY(-1px);
}

.pipeline-panel__engine {
  display: grid;
  place-items: center;
  width: 31px;
  height: 31px;
  color: var(--dm-color-brand);
  border: 1px solid #c7d2fe;
  border-radius: 6px;
  background: var(--dm-color-brand-soft);
}

.pipeline-panel__engine :deep(.app-icon) {
  width: 15px;
  height: 15px;
}

.pipeline-panel__copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.pipeline-panel__copy strong,
.pipeline-panel__copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pipeline-panel__copy strong {
  color: var(--dm-color-zinc-900);
  font-size: 12px;
  font-weight: 650;
}

.pipeline-panel__copy small {
  color: var(--dm-color-zinc-500);
  font-size: 11px;
}

.pipeline-panel__status {
  display: flex;
  grid-column: 1 / -1;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--dm-color-zinc-600);
  font-size: 11px;
}

.pipeline-panel__status > span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.pipeline-panel__status :deep(.app-icon) {
  width: 13px;
  height: 13px;
}

.pipeline-panel__status > strong {
  color: var(--dm-color-brand);
  font-family: var(--dm-font-mono);
  font-size: 11px;
}

.pipeline-panel__progress {
  position: relative;
  grid-column: 1 / -1;
  height: 3px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--dm-color-zinc-200);
}

.pipeline-panel__progress i {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: var(--dm-color-accent);
  transition: width var(--dm-motion-normal) var(--dm-motion-easing);
}

@media (max-width: 700px) {
  .pipeline-panel__list {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .pipeline-panel__progress i,
  .pipeline-panel__list > button {
    transition: none;
  }
}
</style>
