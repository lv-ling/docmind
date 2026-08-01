<script setup lang="ts">
import type { SourceVersionId, Template, WorkspaceId } from '@docmind/contracts';
import { DOCUMENT_MODEL_VERSION } from '@docmind/editor';
import { DmButton, DmStatus } from '@docmind/ui';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { createTemplate, listTemplates } from '../api/templates.js';
import AppIcon from '../components/AppIcon.vue';
import InlineNotice from '../components/InlineNotice.vue';

const route = useRoute();
const router = useRouter();
const templates = ref<Template[]>([]);
const loading = ref(true);
const creating = ref(false);
const error = ref('');
const sourceVersionId = computed(() => {
  const value = route.query.sourceVersionId;
  return typeof value === 'string' && value.length > 0 ? (value as SourceVersionId) : null;
});
const name = ref(
  typeof route.query.suggestedName === 'string' ? route.query.suggestedName : '未命名文档模板',
);
let refreshTimer: ReturnType<typeof setTimeout> | null = null;

const statusTone = (
  status: Template['conversion_status'],
): 'info' | 'success' | 'warning' | 'danger' => {
  if (status === 'ready') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'retrying') return 'warning';
  return 'info';
};

const statusLabel = (status: Template['conversion_status']): string =>
  ({
    queued: '等待转换',
    running: '转换中',
    ready: '可编辑',
    retrying: '等待重试',
    failed: '转换失败',
  })[status];

const scheduleRefresh = (): void => {
  if (refreshTimer !== null) clearTimeout(refreshTimer);
  if (
    templates.value.some((item) =>
      ['queued', 'running', 'retrying'].includes(item.conversion_status),
    )
  ) {
    refreshTimer = setTimeout(load, 3000);
  }
};

const load = async (): Promise<void> => {
  try {
    templates.value = await listTemplates(String(route.params.workspaceId) as WorkspaceId);
    error.value = '';
    scheduleRefresh();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '模板登记簿读取失败';
  } finally {
    loading.value = false;
  }
};

const submit = async (): Promise<void> => {
  if (sourceVersionId.value === null || name.value.trim().length === 0) return;
  creating.value = true;
  error.value = '';
  try {
    const accepted = await createTemplate(sourceVersionId.value, name.value.trim());
    await router.replace({
      name: 'template-editor',
      params: {
        workspaceId: route.params.workspaceId,
        templateId: accepted.template_id,
      },
    });
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '模板转换任务创建失败';
  } finally {
    creating.value = false;
  }
};

onMounted(load);
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

    <InlineNotice v-if="error" tone="danger" title="模板操作未完成" :detail="error" />

    <form v-if="sourceVersionId" class="template-intake" @submit.prevent="submit">
      <div>
        <p class="eyebrow">NEW CONVERSION</p>
        <h2>将所选原件转换为模板</h2>
        <p>转换任务会同时生成 PDF 原件预览、受控文档模型、白名单 HTML 和版式告警。</p>
      </div>
      <label>
        <span>模板名称</span>
        <input v-model="name" required maxlength="200" autocomplete="off" />
        <small>来源版本 {{ sourceVersionId }}</small>
      </label>
      <DmButton type="submit" :loading="creating" loading-label="正在创建转换任务">
        开始安全转换 <AppIcon name="arrow" />
      </DmButton>
    </form>

    <div v-if="loading" class="document-loading">正在读取模板登记簿…</div>
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
          name: 'template-editor',
          params: { workspaceId: route.params.workspaceId, templateId: item.id },
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
          :tone="statusTone(item.conversion_status)"
          :label="statusLabel(item.conversion_status)"
        />
        <strong>{{ item.current_version_id ? '已生成' : '—' }}</strong>
        <AppIcon name="arrow" />
      </RouterLink>
    </section>
    <section v-else class="empty-register">
      <span class="paper-stack" aria-hidden="true"><i></i><i></i><i></i></span>
      <h2>模板登记簿还是空的</h2>
      <p>请从“原始文档”详情页选择一个已上传版本，再点击“转换为模板”。</p>
      <RouterLink :to="`/w/${route.params.workspaceId}/sources`">前往原始文档 →</RouterLink>
    </section>
  </section>
</template>
