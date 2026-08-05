<script setup lang="ts">
import { AppIcon } from '@/components/index.js';

import type { WorkbenchEfficiency, WorkbenchInsight } from '../../model/workbench-overview.js';

defineOptions({ name: 'WorkspaceInsights' });

defineProps<{
  efficiency: WorkbenchEfficiency;
  insight: WorkbenchInsight;
}>();

const emit = defineEmits<{
  'open-config': [];
}>();
</script>

<template>
  <aside class="workspace-insights" aria-label="AI 效能与建议">
    <section class="workspace-insights__dark">
      <span class="workspace-insights__glow" aria-hidden="true"></span>
      <header><AppIcon name="activity" /><span>AI 效能观测</span></header>

      <div class="workspace-insights__headline">
        <strong>{{ efficiency.parsedDocumentCount }}</strong>
        <span>份文档已解析</span>
      </div>
      <p>
        本周自动抽取已为您节省约
        <strong>{{ efficiency.savedHours }} 小时</strong>
        工时。
      </p>

      <dl>
        <div>
          <dt>综合准确率</dt>
          <dd>{{ efficiency.accuracyRate.toFixed(1) }}% <span>↑</span></dd>
        </div>
        <div>
          <dt>自动归档率</dt>
          <dd>{{ efficiency.autoArchiveRate.toFixed(1) }}%</dd>
        </div>
      </dl>
    </section>

    <section class="workspace-insights__notice">
      <header>
        <AppIcon name="lightbulb" />
        <h2>全局发现与建议</h2>
      </header>
      <div>
        <AppIcon name="activity" />
        <span>
          <strong>{{ insight.title }}</strong>
          <p>{{ insight.description }}</p>
          <button type="button" @click="emit('open-config')">
            {{ insight.actionLabel }} <AppIcon name="arrow" />
          </button>
        </span>
      </div>
    </section>
  </aside>
</template>

<style scoped>
.workspace-insights {
  display: grid;
  align-content: start;
  gap: 20px;
}

.workspace-insights__dark,
.workspace-insights__notice {
  position: relative;
  overflow: hidden;
  border-radius: var(--dm-radius-medium);
}

.workspace-insights__dark {
  min-height: 195px;
  padding: 19px 20px;
  color: var(--dm-color-zinc-100);
  border: 1px solid var(--dm-color-zinc-800);
  background: var(--dm-color-zinc-900);
  box-shadow: 0 8px 24px rgb(24 24 27 / 14%);
}

.workspace-insights__glow {
  position: absolute;
  top: -84px;
  right: -72px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgb(99 102 241 / 24%);
  filter: blur(48px);
  pointer-events: none;
}

.workspace-insights__dark header,
.workspace-insights__notice header,
.workspace-insights__notice > div,
.workspace-insights__headline {
  display: flex;
  align-items: center;
}

.workspace-insights__dark header {
  position: relative;
  gap: 7px;
  margin-bottom: 21px;
  color: var(--dm-color-zinc-300);
  font-size: 12px;
  font-weight: 650;
  letter-spacing: 0.02em;
}

.workspace-insights__dark header :deep(.app-icon),
.workspace-insights__notice header :deep(.app-icon) {
  width: 14px;
  height: 14px;
  color: #818cf8;
}

.workspace-insights__headline {
  position: relative;
  align-items: baseline;
  gap: 7px;
}

.workspace-insights__headline strong {
  color: #fff;
  font: 650 31px/1 var(--dm-font-mono);
  letter-spacing: -0.06em;
}

.workspace-insights__headline span,
.workspace-insights__dark > p {
  color: var(--dm-color-zinc-400);
  font-size: 11px;
}

.workspace-insights__dark > p {
  position: relative;
  margin: 8px 0 18px;
  line-height: 1.5;
}

.workspace-insights__dark > p strong {
  color: #a5b4fc;
  font-weight: 650;
}

.workspace-insights__dark dl {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding-top: 13px;
  margin: 0;
  border-top: 1px solid var(--dm-color-zinc-800);
}

.workspace-insights__dark dl div {
  display: grid;
  gap: 5px;
}

.workspace-insights__dark dt {
  color: var(--dm-color-zinc-500);
  font-size: 10px;
  text-transform: uppercase;
}

.workspace-insights__dark dd {
  margin: 0;
  color: var(--dm-color-zinc-200);
  font: 650 13px/1 var(--dm-font-mono);
}

.workspace-insights__dark dd span {
  color: #34d399;
  font-size: 10px;
}

.workspace-insights__notice {
  min-height: 125px;
  padding: 15px 16px;
  border: 1px solid var(--dm-color-border);
  background: #fff;
  box-shadow: var(--dm-shadow-card);
}

.workspace-insights__notice::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: #818cf8;
  content: '';
}

.workspace-insights__notice header {
  gap: 7px;
  margin-bottom: 13px;
}

.workspace-insights__notice h2 {
  margin: 0;
  color: var(--dm-color-zinc-900);
  font-size: 12px;
}

.workspace-insights__notice > div {
  align-items: flex-start;
  gap: 9px;
}

.workspace-insights__notice > div > :deep(.app-icon) {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: #f59e0b;
}

.workspace-insights__notice > div span {
  display: grid;
  gap: 3px;
}

.workspace-insights__notice strong {
  color: var(--dm-color-zinc-900);
  font-size: 12px;
}

.workspace-insights__notice p {
  margin: 0;
  color: var(--dm-color-zinc-500);
  font-size: 10px;
  line-height: 1.55;
}

.workspace-insights__notice button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  justify-self: start;
  padding: 0;
  margin-top: 5px;
  color: var(--dm-color-brand);
  border: 0;
  background: transparent;
  font-size: 10px;
  font-weight: 650;
  cursor: pointer;
}

.workspace-insights__notice button :deep(.app-icon) {
  width: 11px;
  height: 11px;
}
</style>
