<script setup lang="ts">
import { DmButton } from '@/ui';
import { nextTick, ref, watch } from 'vue';

import AppIcon from '@/components/AppIcon.vue';

const props = defineProps<{
  open: boolean;
  kind: 'review' | 'pipeline';
  title: string;
}>();

const emit = defineEmits<{
  close: [];
  'open-documents': [];
}>();

const dialogRef = ref<HTMLElement | null>(null);

watch(
  () => props.open,
  async (isOpen) => {
    if (!isOpen) return;
    await nextTick();
    dialogRef.value?.focus();
  },
);
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="workflow-preview-dialog__backdrop" @click.self="emit('close')">
      <section
        ref="dialogRef"
        class="workflow-preview-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="workflow-preview-title"
        tabindex="-1"
        @keydown.esc="emit('close')"
      >
        <header>
          <span><AppIcon :name="kind === 'review' ? 'list-checks' : 'cpu'" /></span>
          <div>
            <small>{{ kind === 'review' ? 'REVIEW WORKFLOW' : 'PROCESSING PIPELINE' }}</small>
            <h2 id="workflow-preview-title">{{ title }}</h2>
          </div>
          <DmButton variant="ghost" icon-only aria-label="关闭" @click="emit('close')">
            <AppIcon name="close" />
          </DmButton>
        </header>

        <p>
          {{
            kind === 'review'
              ? '复核工作区的前端交互已经就绪；后端审核任务接口接入后，将在这里绑定原件、抽取字段和风险证据。'
              : '处理任务详情的前端状态已经就绪；后端聚合接口接入后，将展示逐阶段耗时、日志和失败重试操作。'
          }}
        </p>

        <footer>
          <DmButton variant="secondary" @click="emit('close')">稍后处理</DmButton>
          <DmButton @click="emit('open-documents')">查看关联文档</DmButton>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.workflow-preview-dialog__backdrop {
  position: fixed;
  z-index: 120;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgb(24 24 27 / 38%);
  backdrop-filter: blur(3px);
}

.workflow-preview-dialog {
  width: min(100%, 480px);
  padding: 18px;
  border: 1px solid var(--dm-color-border);
  border-radius: var(--dm-radius-large);
  outline: 0;
  background: #fff;
  box-shadow: var(--dm-shadow-float);
}

.workflow-preview-dialog > header {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 28px;
  align-items: center;
  gap: 11px;
}

.workflow-preview-dialog > header > span {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  color: var(--dm-color-brand);
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  background: var(--dm-color-brand-soft);
}

.workflow-preview-dialog > header > span :deep(.app-icon) {
  width: 17px;
  height: 17px;
}

.workflow-preview-dialog header div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.workflow-preview-dialog small {
  color: var(--dm-color-zinc-400);
  font: 700 9px/1.2 var(--dm-font-mono);
  letter-spacing: 0.08em;
}

.workflow-preview-dialog h2 {
  overflow: hidden;
  margin: 0;
  color: var(--dm-color-zinc-900);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workflow-preview-dialog > p {
  margin: 18px 0;
  color: var(--dm-color-zinc-600);
  font-size: 12px;
  line-height: 1.7;
}

.workflow-preview-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
