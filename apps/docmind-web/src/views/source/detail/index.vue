<script setup lang="ts">
import type {
  SourceDocumentDetail,
  SourcePreviewAccess,
  SourceVersion,
  SourceVersionId,
} from '@/contracts';
import { DmButton, DmStatus } from '@/ui';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getAuthenticatedObjectUrl } from '@/api/client.js';
import { getSource, getSourcePreview } from '@/api/sources.js';
import { AppIcon, InlineNotice } from '@/components/index.js';
import { RouteName } from '@/router/constants.js';
import { getQueryString } from '@/router/query.js';
import { formatBytes } from '@/utils/file.js';

const route = useRoute();
const router = useRouter();
const sourceDetail = ref<SourceDocumentDetail | null>(null);
const selectedVersionId = ref<SourceVersionId | null>(null);
const previewAccess = ref<SourcePreviewAccess | null>(null);
const previewObjectUrl = ref<string | null>(null);
const originalObjectUrl = ref<string | null>(null);
const isLoadingSource = ref(true);
const isLoadingPreview = ref(false);
const sourceError = ref('');
let previewTimer: ReturnType<typeof setTimeout> | null = null;
const sourceId = computed(() => getQueryString(route.query.sourceId));

const revokePreviewUrls = (): void => {
  if (previewObjectUrl.value !== null) URL.revokeObjectURL(previewObjectUrl.value);
  if (originalObjectUrl.value !== null) URL.revokeObjectURL(originalObjectUrl.value);
  previewObjectUrl.value = null;
  originalObjectUrl.value = null;
};

const selectedVersion = computed<SourceVersion | null>(
  () =>
    sourceDetail.value?.versions.find((version) => version.id === selectedVersionId.value) ?? null,
);

const getStatusTone = (
  status: SourceVersion['status'],
): 'info' | 'success' | 'warning' | 'danger' => {
  if (status === 'ready') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'uploading') return 'warning';
  return 'info';
};

const loadPreview = async (): Promise<void> => {
  if (selectedVersionId.value === null) return;
  isLoadingPreview.value = true;
  try {
    previewAccess.value = await getSourcePreview(selectedVersionId.value);
    if (originalObjectUrl.value === null) {
      originalObjectUrl.value = await getAuthenticatedObjectUrl(
        previewAccess.value.original_content_url,
      );
    }
    if (
      previewAccess.value.preview.status === 'ready' &&
      previewAccess.value.view_url !== null &&
      previewObjectUrl.value === null
    ) {
      previewObjectUrl.value = await getAuthenticatedObjectUrl(previewAccess.value.view_url);
    }
    if (['queued', 'processing'].includes(previewAccess.value.preview.status)) {
      previewTimer = setTimeout(loadPreview, 2500);
    }
  } catch (caught) {
    sourceError.value = caught instanceof Error ? caught.message : '预览加载失败';
  } finally {
    isLoadingPreview.value = false;
  }
};

const selectVersion = async (versionId: SourceVersionId): Promise<void> => {
  if (previewTimer !== null) clearTimeout(previewTimer);
  revokePreviewUrls();
  selectedVersionId.value = versionId;
  previewAccess.value = null;
  await loadPreview();
};

const loadSourceDetail = async (): Promise<void> => {
  isLoadingSource.value = true;
  if (sourceId.value === null) {
    sourceError.value = '缺少文档标识';
    isLoadingSource.value = false;
    return;
  }
  try {
    sourceDetail.value = await getSource(sourceId.value);
    const current =
      sourceDetail.value.source.current_version_id ?? sourceDetail.value.versions[0]?.id ?? null;
    if (current !== null) await selectVersion(current);
  } catch (caught) {
    sourceError.value = caught instanceof Error ? caught.message : '文档详情加载失败';
  } finally {
    isLoadingSource.value = false;
  }
};

const startExtraction = async (): Promise<void> => {
  if (selectedVersionId.value === null) return;
  await router.push({
    name: RouteName.ExtractionCreate,
    query: { sourceVersionId: selectedVersionId.value },
  });
};

const startTemplateConversion = async (): Promise<void> => {
  if (selectedVersionId.value === null) return;
  await router.push({
    name: RouteName.TemplateList,
    query: {
      sourceVersionId: selectedVersionId.value,
      suggestedName: `${sourceDetail.value?.source.name ?? '文档'}模板`,
    },
  });
};

onMounted(loadSourceDetail);
onUnmounted(() => {
  if (previewTimer !== null) clearTimeout(previewTimer);
  revokePreviewUrls();
});
</script>

<template>
  <section class="page-stack">
    <header class="page-heading page-heading--actions">
      <div>
        <button class="back-link" type="button" @click="router.back()">← 返回文档登记簿</button>
        <p class="eyebrow">SOURCE FILE / DETAIL</p>
        <h1>{{ sourceDetail?.source.name ?? '文档详情' }}</h1>
      </div>
      <div class="page-action-group">
        <DmButton
          variant="secondary"
          :disabled="selectedVersion?.status !== 'ready'"
          @click="startTemplateConversion"
        >
          转换为模板
        </DmButton>
        <DmButton :disabled="selectedVersion?.status !== 'ready'" @click="startExtraction">
          发起字段抽取 <AppIcon name="arrow" />
        </DmButton>
      </div>
    </header>
    <InlineNotice v-if="sourceError" tone="danger" title="无法读取文档" :detail="sourceError" />
    <div v-if="isLoadingSource" class="document-loading">正在读取文档登记信息…</div>
    <div v-else-if="sourceDetail" class="document-detail-layout">
      <aside class="version-rail" aria-label="文档版本">
        <p class="rail-title">版本记录</p>
        <button
          v-for="version in sourceDetail.versions"
          :key="version.id"
          type="button"
          :class="{ active: selectedVersionId === version.id }"
          @click="selectVersion(version.id)"
        >
          <span>V{{ version.version_number }}</span>
          <strong>{{ version.original_file_name }}</strong>
          <small>{{ new Date(version.created_at).toLocaleDateString('zh-CN') }}</small>
          <DmStatus :label="version.status" :tone="getStatusTone(version.status)" />
        </button>
      </aside>
      <section class="preview-stage">
        <header>
          <div>
            <p class="eyebrow">IMMUTABLE ORIGINAL</p>
            <strong>{{ selectedVersion?.original_file_name }}</strong>
          </div>
          <div v-if="selectedVersion" class="file-facts">
            <span>{{
              selectedVersion.file?.size_bytes
                ? formatBytes(selectedVersion.file.size_bytes)
                : '待校验'
            }}</span>
            <span>{{ selectedVersion.file_type.toUpperCase() }}</span>
          </div>
        </header>
        <div class="preview-frame">
          <iframe
            v-if="previewAccess?.preview.status === 'ready' && previewObjectUrl"
            :src="previewObjectUrl"
            title="文档 PDF 预览"
          ></iframe>
          <div
            v-else-if="previewAccess?.preview.status === 'failed'"
            class="preview-placeholder preview-placeholder--error"
          >
            <strong>预览生成失败</strong><span>仍可下载不可变原件或直接发起抽取。</span>
          </div>
          <div v-else class="preview-placeholder">
            <span class="paper-stack" aria-hidden="true"><i></i><i></i><i></i></span>
            <strong>{{ isLoadingPreview ? '正在请求预览' : '正在生成文档预览' }}</strong>
            <span>DOC/DOCX 将由后端转换为安全的 PDF 预览。</span>
          </div>
        </div>
        <footer v-if="previewAccess">
          <a
            v-if="originalObjectUrl"
            :href="originalObjectUrl"
            :download="selectedVersion?.original_file_name"
            >查看不可变原件 ↗</a
          >
          <span v-if="previewAccess.preview.page_count">
            {{ previewAccess.preview.page_count }} 页
          </span>
        </footer>
      </section>
    </div>
  </section>
</template>

<style src="./styles.css"></style>
