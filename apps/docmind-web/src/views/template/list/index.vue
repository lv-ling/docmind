<script setup lang="ts">
import type { SourceVersionId, Template, WorkspaceId } from '@/contracts';
import { DOCUMENT_MODEL_VERSION } from '@/editor';
import { DmButton, DmStatus } from '@/ui';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { createTemplate, listTemplates } from '@/api/templates.js';
import AppIcon from '@/components/AppIcon.vue';
import InlineNotice from '@/components/InlineNotice.vue';
import { RouteName } from '@/router/constants.js';
import { getQueryString } from '@/router/query.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

const route = useRoute();
const router = useRouter();
const workspace = useWorkspaceStore();
const workspaceId = computed(() => workspace.selectedId as WorkspaceId);
const templates = ref<Template[]>([]);
const isLoadingTemplates = ref(true);
const isCreatingTemplate = ref(false);
const templateError = ref('');
const sourceVersionId = computed(() => {
  const value = getQueryString(route.query.sourceVersionId);
  return value === null ? null : (value as SourceVersionId);
});
const templateName = ref(getQueryString(route.query.suggestedName) ?? '未命名文档模板');
let refreshTimer: ReturnType<typeof setTimeout> | null = null;

const getStatusTone = (
  status: Template['conversion_status'],
): 'info' | 'success' | 'warning' | 'danger' => {
  if (status === 'ready') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'retrying') return 'warning';
  return 'info';
};

const getStatusLabel = (status: Template['conversion_status']): string =>
  ({
    queued: '等待转换',
    running: '转换中',
    ready: '可编辑',
    retrying: '等待重试',
    failed: '转换失败',
  })[status];

const scheduleTemplateRefresh = (): void => {
  if (refreshTimer !== null) clearTimeout(refreshTimer);
  if (
    templates.value.some((item) =>
      ['queued', 'running', 'retrying'].includes(item.conversion_status),
    )
  ) {
    refreshTimer = setTimeout(loadTemplates, 3000);
  }
};

const loadTemplates = async (): Promise<void> => {
  try {
    templates.value = await listTemplates(workspaceId.value);
    templateError.value = '';
    scheduleTemplateRefresh();
  } catch (caught) {
    templateError.value = caught instanceof Error ? caught.message : '模板登记簿读取失败';
  } finally {
    isLoadingTemplates.value = false;
  }
};

const handleCreateTemplate = async (): Promise<void> => {
  if (sourceVersionId.value === null || templateName.value.trim().length === 0) return;
  isCreatingTemplate.value = true;
  templateError.value = '';
  try {
    const accepted = await createTemplate(sourceVersionId.value, templateName.value.trim());
    await router.replace({
      name: RouteName.TemplateEditor,
      query: { templateId: accepted.template_id },
    });
  } catch (caught) {
    templateError.value = caught instanceof Error ? caught.message : '模板转换任务创建失败';
  } finally {
    isCreatingTemplate.value = false;
  }
};

watch(workspaceId, () => void loadTemplates());

onMounted(loadTemplates);
onUnmounted(() => {
  if (refreshTimer !== null) clearTimeout(refreshTimer);
});
</script>

<template>
  <section class="page-stack">
    <header class="page-heading page-heading--actions">
      <div>
        <p class="eyebrow">TEMPLATE STUDIO / CONTROLLED DOCUMENT</p>
        <h1>文档模板</h1>
        <p class="page-lead">从不可变原件生成可审查、可微调、可回滚的受控模板。</p>
      </div>
      <span class="model-version-stamp">MODEL {{ DOCUMENT_MODEL_VERSION }}</span>
    </header>

    <InlineNotice
      v-if="templateError"
      tone="danger"
      title="模板操作未完成"
      :detail="templateError"
    />

    <form v-if="sourceVersionId" class="template-intake" @submit.prevent="handleCreateTemplate">
      <div>
        <p class="eyebrow">NEW CONVERSION</p>
        <h2>将所选原件转换为模板</h2>
        <p>转换任务会同时生成 PDF 原件预览、受控文档模型、白名单 HTML 和版式告警。</p>
      </div>
      <label>
        <span>模板名称</span>
        <input v-model="templateName" required maxlength="200" autocomplete="off" />
        <small>来源版本 {{ sourceVersionId }}</small>
      </label>
      <DmButton type="submit" :loading="isCreatingTemplate" loading-label="正在创建转换任务">
        开始安全转换 <AppIcon name="arrow" />
      </DmButton>
    </form>

    <div v-if="isLoadingTemplates" class="document-loading">正在读取模板登记簿…</div>
    <section v-else-if="templates.length > 0" class="template-register">
      <header class="register-heading">
        <span>模板名称</span><span>来源版本</span><span>转换状态</span><span>当前版本</span
        ><span></span>
      </header>
      <RouterLink
        v-for="item in templates"
        :key="item.id"
        class="template-register-row"
        :to="{
          name: RouteName.TemplateEditor,
          query: { templateId: item.id },
        }"
      >
        <span class="template-name-cell">
          <i aria-hidden="true">T</i>
          <span
            ><strong>{{ item.name }}</strong
            ><small>{{ item.id }}</small></span
          >
        </span>
        <code>{{ item.source_version_id.slice(0, 8) }}…</code>
        <DmStatus
          :tone="getStatusTone(item.conversion_status)"
          :label="getStatusLabel(item.conversion_status)"
        />
        <strong>{{ item.current_version_id ? '已生成' : '—' }}</strong>
        <AppIcon name="arrow" />
      </RouterLink>
    </section>
    <section v-else class="empty-register">
      <span class="paper-stack" aria-hidden="true"><i></i><i></i><i></i></span>
      <h2>模板登记簿还是空的</h2>
      <p>请从“原始文档”详情页选择一个已上传版本，再点击“转换为模板”。</p>
      <RouterLink :to="{ name: RouteName.SourceList }">前往原始文档 →</RouterLink>
    </section>
  </section>
</template>

<style src="./styles.css"></style>
