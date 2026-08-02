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
  <section
    class="workbench-overview mx-auto grid w-full max-w-content gap-6 p-6 motion-reduce:animate-none max-[700px]:gap-[18px] max-[700px]:px-3.5 max-[700px]:py-[18px]"
  >
    <header
      class="workbench-overview__header flex items-end justify-between gap-6 max-[700px]:flex-col max-[700px]:items-start max-[700px]:gap-3.5"
    >
      <div>
        <h1 class="m-0 text-xl leading-7 font-semibold tracking-tight text-zinc-900">工作台</h1>
        <p
          class="mt-1 mb-0 flex items-center gap-1.5 text-[13px] leading-[19.5px] text-zinc-500 max-[700px]:items-start max-[700px]:text-[12px]"
        >
          <AppIcon class="size-3.5 shrink-0 text-brand-500" name="sparkles" />
          <span>
            AI 已自动处理
            <strong class="font-medium text-zinc-900">
              {{ overview.activitySummary.processedDocumentCount }}
            </strong>
            份新文档。发现
            <mark class="rounded-xs bg-[#fffbeb] px-1 font-medium text-[#d97706]">
              {{ overview.activitySummary.attentionCount }} 份
            </mark>
            业务异常，需复核。
          </span>
        </p>
      </div>
      <div class="workbench-overview__actions flex shrink-0 gap-2">
        <DmButton variant="secondary" size="small" @click="handleOpenReviewQueue">
          <AppIcon class="text-zinc-400" name="list-checks" />审核队列
        </DmButton>
        <DmButton size="small" @click="handleStartContinuousReview">
          <AppIcon name="play" />连续复核
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

    <div
      v-else
      class="workbench-overview__grid grid grid-cols-12 items-start gap-6 max-[1050px]:grid-cols-1"
    >
      <div
        class="workbench-overview__primary col-span-8 grid min-w-0 gap-6 max-[1050px]:col-span-1"
      >
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

    <DmButton v-if="overviewError" variant="ghost" class="justify-self-start" @click="loadOverview">
      重新连接实时数据
    </DmButton>

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
  animation: overview-reveal 360ms var(--dm-motion-easing) both;
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
  .workbench-overview__loading {
    grid-template-columns: 1fr;
  }

  .workbench-overview__loading i:nth-child(2),
  .workbench-overview__loading i:nth-child(3) {
    grid-column: 1;
    grid-row: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .workbench-overview,
  .workbench-overview__loading i {
    animation: none;
  }
}
</style>
