<script setup lang="ts">
import { DmButton } from '@/ui';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import InlineNotice from '@/components/InlineNotice.vue';
import { RouteName } from '@/router/constants.js';

import AttentionQueue from './components/AttentionQueue.vue';
import PipelinePanel from './components/PipelinePanel.vue';
import WorkflowPreviewDialog from './components/WorkflowPreviewDialog.vue';
import WorkspaceInsights from './components/WorkspaceInsights.vue';
import { useWorkbenchOverview } from './composables/useWorkbenchOverview.js';
import type { WorkbenchAttentionItem, WorkbenchPipelineItem } from './model/workbench-overview.js';

interface WorkflowPreviewSelection {
  kind: 'review' | 'pipeline';
  title: string;
}

const router = useRouter();
const { overview, isLoadingOverview, overviewError, loadOverview } = useWorkbenchOverview();
const workflowPreviewSelection = ref<WorkflowPreviewSelection | null>(null);

const handleOpenReviewQueue = (): void => {
  document.querySelector('#workbench-attention')?.scrollIntoView({ behavior: 'smooth' });
};

const handleStartContinuousReview = (): void => {
  const firstReviewItem = overview.value.attentionItems[0];
  if (firstReviewItem === undefined) return;
  workflowPreviewSelection.value = { kind: 'review', title: firstReviewItem.title };
};

const handleOpenAttentionItem = (item: WorkbenchAttentionItem): void => {
  workflowPreviewSelection.value = { kind: 'review', title: item.title };
};

const handleOpenPipelineItem = (item: WorkbenchPipelineItem): void => {
  workflowPreviewSelection.value = { kind: 'pipeline', title: item.title };
};

const handleOpenDocuments = async (): Promise<void> => {
  workflowPreviewSelection.value = null;
  await router.push({ name: RouteName.SourceList });
};

const handleOpenConfig = async (): Promise<void> => {
  await router.push({ name: RouteName.SchemaList });
};
</script>

<template>
  <section class="workbench-overview">
    <header class="workbench-overview__header">
      <div>
        <h1>工作台</h1>
        <p>
          <AppIcon name="sparkles" />
          <span>
            AI 已自动处理
            <strong>{{ overview.activitySummary.processedDocumentCount }}</strong>
            份新文档。发现
            <mark>{{ overview.activitySummary.attentionCount }} 份</mark>
            业务异常，需复核。
          </span>
        </p>
      </div>
      <div class="workbench-overview__actions">
        <DmButton variant="secondary" size="small" @click="handleOpenReviewQueue">
          <AppIcon name="review" />审核队列
        </DmButton>
        <DmButton size="small" @click="handleStartContinuousReview">
          <AppIcon name="arrow" />连续复核
        </DmButton>
      </div>
    </header>

    <InlineNotice
      v-if="overviewError"
      tone="danger"
      title="实时数据加载失败"
      detail="当前展示前端演示工作流；可重新连接文档、模板和字段配置接口。"
    />

    <div v-if="isLoadingOverview" class="workbench-overview__loading" aria-label="正在加载工作台">
      <i></i><i></i><i></i>
    </div>

    <div v-else class="workbench-overview__grid">
      <div class="workbench-overview__primary">
        <AttentionQueue
          :items="overview.attentionItems"
          :attention-count="overview.activitySummary.attentionCount"
          @open-item="handleOpenAttentionItem"
        />
        <PipelinePanel :items="overview.pipelineItems" @open-item="handleOpenPipelineItem" />
      </div>
      <WorkspaceInsights
        :efficiency="overview.efficiency"
        :insight="overview.insight"
        @open-config="handleOpenConfig"
      />
    </div>

    <button
      v-if="overviewError"
      class="workbench-overview__retry"
      type="button"
      @click="loadOverview"
    >
      重新连接实时数据
    </button>

    <WorkflowPreviewDialog
      :open="workflowPreviewSelection !== null"
      :kind="workflowPreviewSelection?.kind ?? 'review'"
      :title="workflowPreviewSelection?.title ?? ''"
      @close="workflowPreviewSelection = null"
      @open-documents="handleOpenDocuments"
    />
  </section>
</template>

<style scoped>
.workbench-overview {
  display: grid;
  gap: 24px;
  width: min(100%, 1600px);
  padding: 24px;
  margin: 0 auto;
  animation: overview-reveal 360ms var(--dm-motion-easing) both;
}

.workbench-overview__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.workbench-overview__header h1 {
  margin: 0;
  color: var(--dm-color-zinc-900);
  font-size: 20px;
  font-weight: 680;
  letter-spacing: -0.025em;
}

.workbench-overview__header p {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 5px 0 0;
  color: var(--dm-color-zinc-500);
  font-size: 13px;
  line-height: 1.45;
}

.workbench-overview__header p :deep(.app-icon) {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: var(--dm-color-accent);
}

.workbench-overview__header p strong {
  color: var(--dm-color-zinc-900);
  font-weight: 650;
}

.workbench-overview__header p mark {
  padding: 1px 5px;
  color: #b45309;
  border-radius: 4px;
  background: #fffbeb;
  font-weight: 650;
}

.workbench-overview__actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

.workbench-overview__actions :deep(.dm-button) {
  min-height: 32px;
  padding: 7px 12px;
  font-size: 12px;
}

.workbench-overview__actions :deep(.app-icon) {
  width: 14px;
  height: 14px;
}

.workbench-overview__grid {
  display: grid;
  grid-template-columns: minmax(0, 2.06fr) minmax(300px, 1fr);
  gap: 24px;
  align-items: start;
}

.workbench-overview__primary {
  display: grid;
  gap: 24px;
  min-width: 0;
}

.workbench-overview__loading {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 24px;
}

.workbench-overview__loading i {
  min-height: 195px;
  border: 1px solid var(--dm-color-border);
  border-radius: var(--dm-radius-medium);
  background: linear-gradient(
    100deg,
    var(--dm-color-zinc-50) 20%,
    var(--dm-color-zinc-100) 40%,
    var(--dm-color-zinc-50) 60%
  );
  background-size: 220% 100%;
  animation: overview-loading 1.5s linear infinite;
}

.workbench-overview__loading i:nth-child(2) {
  grid-column: 2;
  grid-row: 1;
}

.workbench-overview__loading i:nth-child(3) {
  min-height: 76px;
  grid-column: 1;
}

.workbench-overview__retry {
  justify-self: start;
  padding: 0;
  color: var(--dm-color-brand);
  border: 0;
  background: transparent;
  font-size: 11px;
  font-weight: 650;
  cursor: pointer;
}

@keyframes overview-reveal {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
}

@keyframes overview-loading {
  to {
    background-position: -220% 0;
  }
}

@media (max-width: 1050px) {
  .workbench-overview__grid,
  .workbench-overview__loading {
    grid-template-columns: 1fr;
  }

  .workbench-overview__loading i:nth-child(2),
  .workbench-overview__loading i:nth-child(3) {
    grid-column: 1;
    grid-row: auto;
  }
}

@media (max-width: 700px) {
  .workbench-overview {
    gap: 18px;
    padding: 18px 14px;
  }

  .workbench-overview__header {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }

  .workbench-overview__header p {
    align-items: flex-start;
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .workbench-overview,
  .workbench-overview__loading i {
    animation: none;
  }
}
</style>
