<script setup lang="ts">
import type { SourceDocument, SourceVersionId, WorkspaceId } from '@/contracts';
import { DmButton, DmStatus, DmTextField } from '@/ui';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { uploadFileDirectly } from '../api/client.js';
import { completeSourceUpload, createSourceUpload, listSources } from '../api/sources.js';
import AppIcon from '../components/AppIcon.vue';
import InlineNotice from '../components/InlineNotice.vue';
import { RouteName } from '../router/constants.js';
import { useWorkspaceStore } from '../stores/workspace.js';
import { formatBytes, sha256Hex, validateSourceFile } from '../utils/file.js';

type UploadStage = 'idle' | 'hashing' | 'uploading' | 'verifying' | 'done';
type SourceFilter = 'all' | 'registered' | 'pending';

const SOURCE_PAGE_SIZE = 6;

const route = useRoute();
const router = useRouter();
const workspace = useWorkspaceStore();
const workspaceId = computed(() => workspace.selectedId as WorkspaceId);
const sources = ref<SourceDocument[]>([]);
const loading = ref(true);
const loadError = ref('');
const selectedFile = ref<File | null>(null);
const documentName = ref('');
const uploadStage = ref<UploadStage>('idle');
const progress = ref(0);
const uploadError = ref('');
const dragActive = ref(false);
const input = ref<HTMLInputElement | null>(null);
const searchQuery = ref('');
const sourceFilter = ref<SourceFilter>('all');
const currentPage = ref(1);
const uploadModalOpen = ref(false);
const selectedSourceId = ref<string | null>(null);

const uploading = computed(() => !['idle', 'done'].includes(uploadStage.value));
const stageLabel = computed(() => {
  const labels: Record<UploadStage, string> = {
    idle: '等待上传',
    hashing: '正在计算文件指纹',
    uploading: `正在上传 ${progress.value}%`,
    verifying: '服务端正在校验文件',
    done: '上传完成',
  };
  return labels[uploadStage.value];
});

const filteredSources = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase('zh-CN');
  return sources.value.filter((source) => {
    const matchesQuery =
      query.length === 0 || source.name.toLocaleLowerCase('zh-CN').includes(query);
    const matchesFilter =
      sourceFilter.value === 'all' ||
      (sourceFilter.value === 'registered' && source.current_version_id !== null) ||
      (sourceFilter.value === 'pending' && source.current_version_id === null);
    return matchesQuery && matchesFilter;
  });
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredSources.value.length / SOURCE_PAGE_SIZE)),
);
const safeCurrentPage = computed(() => Math.min(currentPage.value, totalPages.value));
const pagedSources = computed(() => {
  const start = (safeCurrentPage.value - 1) * SOURCE_PAGE_SIZE;
  return filteredSources.value.slice(start, start + SOURCE_PAGE_SIZE);
});
const pageStart = computed(() =>
  filteredSources.value.length === 0 ? 0 : (safeCurrentPage.value - 1) * SOURCE_PAGE_SIZE + 1,
);
const pageEnd = computed(() =>
  Math.min(safeCurrentPage.value * SOURCE_PAGE_SIZE, filteredSources.value.length),
);
const pageNumbers = computed(() =>
  Array.from({ length: totalPages.value }, (_, index) => index + 1),
);
const selectedSource = computed(
  () => sources.value.find((source) => source.id === selectedSourceId.value) ?? null,
);

const formatSourceDate = (value: string): string =>
  new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

const sourceVersionLabel = (source: SourceDocument): string =>
  source.current_version_id?.slice(0, 8) ?? '待上传';

const selectSource = (source: SourceDocument): void => {
  selectedSourceId.value = source.id;
};

const openSelectedSource = async (): Promise<void> => {
  if (selectedSource.value === null) return;
  await router.push({
    name: RouteName.SourceDetail,
    query: { sourceId: selectedSource.value.id },
  });
};

const startSelectedExtraction = async (): Promise<void> => {
  const versionId = selectedSource.value?.current_version_id;
  if (versionId === null || versionId === undefined) return;
  await router.push({
    name: RouteName.ExtractionCreate,
    query: { sourceVersionId: versionId },
  });
};

const resetFilters = (): void => {
  searchQuery.value = '';
  sourceFilter.value = 'all';
};

const closeUploadModal = (): void => {
  if (!uploading.value) uploadModalOpen.value = false;
};

const handleUploadRouteRequest = async (value: unknown): Promise<void> => {
  if (value !== '1') return;
  uploadModalOpen.value = true;
  const nextQuery = { ...route.query };
  delete nextQuery.upload;
  await router.replace({ query: nextQuery });
};

watch([searchQuery, sourceFilter], () => {
  currentPage.value = 1;
  const nextSources = filteredSources.value;
  if (!nextSources.some((source) => source.id === selectedSourceId.value)) {
    selectedSourceId.value = nextSources[0]?.id ?? null;
  }
});

watch(
  () => route.query.q,
  (value) => {
    searchQuery.value = typeof value === 'string' ? value : '';
  },
  { immediate: true },
);

watch(
  () => route.query.upload,
  (value) => void handleUploadRouteRequest(value),
  { immediate: true },
);

const load = async (): Promise<void> => {
  loading.value = true;
  loadError.value = '';
  try {
    const page = await listSources(workspaceId.value);
    sources.value = page.items;
    currentPage.value = 1;
    if (
      selectedSourceId.value === null ||
      !page.items.some((source) => source.id === selectedSourceId.value)
    ) {
      selectedSourceId.value = page.items[0]?.id ?? null;
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '文档列表加载失败';
  } finally {
    loading.value = false;
  }
};

watch(workspaceId, () => void load());

const chooseFile = (file: File): void => {
  uploadError.value = '';
  try {
    validateSourceFile(file);
    selectedFile.value = file;
    documentName.value = file.name.replace(/\.(docx?|pdf)$/i, '');
    uploadStage.value = 'idle';
    progress.value = 0;
  } catch (error) {
    selectedFile.value = null;
    uploadError.value = error instanceof Error ? error.message : '文件不符合要求';
  }
};

const onFileInput = (event: Event): void => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (file !== undefined) chooseFile(file);
};

const onDrop = (event: DragEvent): void => {
  dragActive.value = false;
  const file = event.dataTransfer?.files[0];
  if (file !== undefined) chooseFile(file);
};

const upload = async (): Promise<void> => {
  if (selectedFile.value === null || documentName.value.trim().length === 0) return;
  uploadError.value = '';
  const file = selectedFile.value;
  try {
    const validated = validateSourceFile(file);
    uploadStage.value = 'hashing';
    const sha256 = await sha256Hex(file);
    const session = await createSourceUpload(workspaceId.value, {
      document_name: documentName.value.trim(),
      original_file_name: file.name,
      declared_mime_type: file.type || validated.mimeType,
      size_bytes: file.size,
    });
    if (session.upload.upload_url === null) throw new Error('上传会话已失效，请重新选择文件');

    uploadStage.value = 'uploading';
    const uploaded = await uploadFileDirectly(
      session.upload.upload_url,
      file,
      session.upload.required_headers,
      (state) => (progress.value = state.percentage),
    );
    uploadStage.value = 'verifying';
    await completeSourceUpload(session.version.id as SourceVersionId, {
      size_bytes: file.size,
      detected_mime_type: validated.mimeType,
      sha256,
      object_etag: uploaded.etag,
    });
    uploadStage.value = 'done';
    selectedFile.value = null;
    documentName.value = '';
    if (input.value !== null) input.value.value = '';
    await load();
    uploadModalOpen.value = false;
  } catch (error) {
    uploadStage.value = 'idle';
    uploadError.value = error instanceof Error ? error.message : '上传失败，请重试';
  }
};

onMounted(load);
</script>

<template>
  <section class="page-stack source-page">
    <header class="page-heading source-page-heading">
      <div>
        <h1>文档中心</h1>
        <p>集中管理不可变原件、版本和处理入口。</p>
      </div>
      <DmButton type="button" size="small" @click="uploadModalOpen = true">
        <AppIcon name="plus" /><span>上传文档</span>
      </DmButton>
    </header>

    <div class="source-workbench">
      <section class="source-register" aria-label="文档登记簿">
        <div class="source-list-controls">
          <div class="source-tabs" role="group" aria-label="文档状态筛选">
            <button
              type="button"
              :class="{ active: sourceFilter === 'all' }"
              @click="sourceFilter = 'all'"
            >
              全部文档 <span>{{ sources.length }}</span>
            </button>
            <button
              type="button"
              :class="{ active: sourceFilter === 'registered' }"
              @click="sourceFilter = 'registered'"
            >
              已登记
            </button>
            <button
              type="button"
              :class="{ active: sourceFilter === 'pending' }"
              @click="sourceFilter = 'pending'"
            >
              待上传
            </button>
          </div>
          <div class="source-control-actions">
            <label class="source-search">
              <AppIcon name="search" aria-hidden="true" />
              <span class="dm-sr-only">搜索文档</span>
              <input v-model="searchQuery" type="search" placeholder="搜索文档名称" />
            </label>
            <button
              class="source-refresh"
              type="button"
              :disabled="loading"
              aria-label="刷新文档列表"
              @click="load"
            >
              <AppIcon name="refresh" />
            </button>
          </div>
        </div>

        <div class="source-list-actions">
          <span v-if="selectedSource">已选择：{{ selectedSource.name }}</span>
          <span v-else>选择文档后可查看任务信息</span>
          <span>当前载入 {{ sources.length }} 份</span>
        </div>

        <div class="source-table-head" aria-hidden="true">
          <span>文档名称</span><span>状态</span><span>更新时间</span><span>来源版本</span>
        </div>
        <div class="source-list-viewport">
          <InlineNotice v-if="loadError" tone="danger" title="列表加载失败" :detail="loadError" />
          <div v-if="loading" class="skeleton-list" aria-label="正在加载文档">
            <i v-for="index in 4" :key="index"></i>
          </div>
          <div v-else-if="sources.length === 0" class="empty-register">
            <span>00</span><strong>还没有原始文档</strong>
            <p>上传第一份文档后，将在这里显示版本与处理入口。</p>
          </div>
          <div v-else-if="filteredSources.length === 0" class="source-no-results">
            <strong>没有找到匹配的文档</strong>
            <p>试试更短的名称，或清除当前筛选条件。</p>
            <button type="button" class="text-action" @click="resetFilters">显示全部文档</button>
          </div>
          <ol v-else class="source-list">
            <li v-for="source in pagedSources" :key="source.id">
              <button
                type="button"
                class="source-row"
                :class="{ 'source-row--selected': selectedSourceId === source.id }"
                :aria-pressed="selectedSourceId === source.id"
                @click="selectSource(source)"
              >
                <span class="source-file-copy">
                  <span class="file-glyph"><AppIcon name="document" /></span>
                  <span class="source-name"
                    ><strong>{{ source.name }}</strong
                    ><small>不可变原件</small></span
                  >
                </span>
                <DmStatus
                  :label="source.current_version_id === null ? '待上传' : '已登记'"
                  :tone="source.current_version_id === null ? 'warning' : 'success'"
                />
                <span class="source-updated-at">{{ formatSourceDate(source.updated_at) }}</span>
                <code>{{ sourceVersionLabel(source) }}</code>
              </button>
            </li>
          </ol>
        </div>
        <footer class="source-register-footer">
          <div class="source-list-summary" aria-live="polite">
            <span v-if="filteredSources.length"
              >共 {{ filteredSources.length }} 条 · 显示 {{ pageStart }}–{{ pageEnd }}</span
            >
            <span v-else>没有匹配的文档</span>
            <button
              v-if="searchQuery || sourceFilter !== 'all'"
              type="button"
              class="text-action"
              @click="resetFilters"
            >
              清除筛选
            </button>
          </div>
          <nav
            v-if="filteredSources.length > 0 && totalPages > 1"
            class="source-pagination"
            aria-label="文档列表分页"
          >
            <button
              type="button"
              aria-label="上一页"
              :disabled="safeCurrentPage === 1"
              @click="currentPage = safeCurrentPage - 1"
            >
              上一页
            </button>
            <button
              v-for="page in pageNumbers"
              :key="page"
              type="button"
              :class="{ active: safeCurrentPage === page }"
              :aria-current="safeCurrentPage === page ? 'page' : undefined"
              :aria-label="`第 ${page} 页`"
              @click="currentPage = page"
            >
              {{ page }}
            </button>
            <button
              type="button"
              aria-label="下一页"
              :disabled="safeCurrentPage === totalPages"
              @click="currentPage = safeCurrentPage + 1"
            >
              下一页
            </button>
          </nav>
        </footer>
      </section>

      <aside v-if="selectedSource" class="source-task-panel" aria-label="当前文档任务">
        <header>
          <div>
            <p class="eyebrow">CURRENT DOCUMENT</p>
            <h2>文档信息</h2>
          </div>
          <DmStatus
            :label="selectedSource.current_version_id === null ? '待上传' : '已登记'"
            :tone="selectedSource.current_version_id === null ? 'warning' : 'success'"
          />
        </header>

        <section class="source-task-identity">
          <span class="file-glyph"><AppIcon name="document" /></span>
          <div>
            <strong>{{ selectedSource.name }}</strong
            ><small>不可变原件</small>
          </div>
        </section>

        <section class="source-task-section">
          <h3>原件信息</h3>
          <dl>
            <div>
              <dt>来源版本</dt>
              <dd>
                <code>{{ sourceVersionLabel(selectedSource) }}</code>
              </dd>
            </div>
            <div>
              <dt>登记时间</dt>
              <dd>{{ formatSourceDate(selectedSource.created_at) }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatSourceDate(selectedSource.updated_at) }}</dd>
            </div>
          </dl>
        </section>

        <section class="source-task-section">
          <h3>处理进度</h3>
          <ol class="source-task-steps">
            <li class="is-complete"><span>01</span><strong>原件登记</strong></li>
            <li class="is-current"><span>02</span><strong>字段配置</strong></li>
            <li><span>03</span><strong>抽取复核</strong></li>
            <li><span>04</span><strong>完成</strong></li>
          </ol>
        </section>

        <section class="source-task-section source-task-config">
          <h3>版本与规则</h3>
          <div><span>字段配置版本</span><strong>发起抽取时选择</strong></div>
          <div><span>敏感规则版本</span><strong>发起抽取时选择</strong></div>
        </section>

        <footer>
          <p>任务将绑定当前不可变原件版本，后续配置变更不会影响已创建的任务。</p>
          <DmButton
            variant="secondary"
            :disabled="selectedSource.current_version_id === null"
            @click="openSelectedSource"
          >
            查看文档详情
          </DmButton>
          <DmButton
            :disabled="selectedSource.current_version_id === null"
            @click="startSelectedExtraction"
          >
            发起抽取 <AppIcon name="arrow" />
          </DmButton>
        </footer>
      </aside>

      <aside v-else class="source-task-panel source-task-panel--empty" aria-label="当前文档任务">
        <strong>选择一份文档</strong><span>右侧将显示版本、处理状态与后续操作。</span>
      </aside>
    </div>

    <Teleport to="body">
      <div
        v-if="uploadModalOpen"
        class="upload-modal-backdrop"
        role="presentation"
        @mousedown.self="closeUploadModal"
      >
        <section
          class="upload-panel upload-panel--modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="upload-title"
        >
          <header class="upload-modal-header">
            <div>
              <p class="section-index">NEW SOURCE</p>
              <h2 id="upload-title">上传文档</h2>
            </div>
            <div>
              <DmStatus
                :label="stageLabel"
                :tone="uploadStage === 'done' ? 'success' : uploading ? 'info' : 'neutral'"
                live
              />
              <button
                type="button"
                class="upload-modal-close"
                aria-label="关闭上传窗口"
                @click="closeUploadModal"
              >
                ×
              </button>
            </div>
          </header>
          <div
            class="drop-zone"
            :class="{
              'drop-zone--active': dragActive,
              'drop-zone--selected': selectedFile !== null,
            }"
            @dragenter.prevent="dragActive = true"
            @dragover.prevent
            @dragleave.prevent="dragActive = false"
            @drop.prevent="onDrop"
          >
            <input
              ref="input"
              type="file"
              accept=".doc,.docx,.pdf"
              aria-label="选择 DOC、DOCX 或 PDF 文件"
              @change="onFileInput"
            />
            <AppIcon name="upload" />
            <template v-if="selectedFile === null">
              <strong>拖入文档，或点击浏览</strong>
              <span>DOC / DOCX / PDF · 最大 10 MB</span>
            </template>
            <template v-else>
              <strong>{{ selectedFile.name }}</strong>
              <span>{{ formatBytes(selectedFile.size) }} · 点击可重新选择</span>
            </template>
          </div>
          <DmTextField
            id="source-document-name"
            v-model="documentName"
            label="文档名称"
            description="用于工作台检索，不会修改原始文件名。"
            :disabled="uploading"
            required
          />
          <div v-if="uploading" class="upload-progress" aria-live="polite">
            <span
              :style="{
                width: `${uploadStage === 'uploading' ? progress : uploadStage === 'hashing' ? 12 : 100}%`,
              }"
            ></span>
          </div>
          <InlineNotice v-if="uploadError" tone="danger" title="上传未完成" :detail="uploadError" />
          <DmButton
            :disabled="selectedFile === null || documentName.trim().length === 0"
            :loading="uploading"
            @click="upload"
          >
            上传并校验
          </DmButton>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.source-page {
  display: grid;
  gap: 0;
  width: 100%;
  max-width: none;
  min-height: 100%;
  margin: 0;
  color: var(--dm-color-zinc-900);
}

.source-page-heading {
  position: sticky;
  z-index: 12;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  padding: 11px 22px;
  border: 0;
  border-bottom: 1px solid var(--dm-color-border);
  background: rgb(255 255 255 / 92%);
  backdrop-filter: blur(12px);
}

.source-page-heading::after {
  display: none;
}

.source-page-heading h1 {
  margin: 0;
  color: var(--dm-color-zinc-900);
  font-family: var(--dm-font-ui);
  font-size: 15px;
  font-weight: 680;
  letter-spacing: -0.015em;
}

.source-page-heading p {
  margin: 3px 0 0;
  color: var(--dm-color-zinc-500);
  font-size: 10px;
  line-height: 1.3;
}

.source-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
  width: min(100%, 1500px);
  padding: 20px 22px 24px;
  margin: 0 auto;
}

.source-register,
.source-task-panel {
  overflow: hidden;
  border: 1px solid var(--dm-color-border);
  border-radius: var(--dm-radius-medium);
  background: #fff;
  box-shadow: var(--dm-shadow-card);
}

.source-register {
  display: grid;
  grid-template-rows: auto auto auto minmax(280px, 1fr) auto;
  min-width: 0;
  min-height: 570px;
  padding: 0;
}

.source-list-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 52px;
  padding: 0 14px;
  border-bottom: 1px solid var(--dm-color-border);
}

.source-tabs,
.source-control-actions {
  display: flex;
  align-items: center;
}

.source-tabs {
  align-self: stretch;
  gap: 18px;
}

.source-tabs button {
  position: relative;
  align-self: stretch;
  padding: 0;
  color: var(--dm-color-zinc-500);
  border: 0;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.source-tabs button::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: transparent;
  content: '';
}

.source-tabs button.active {
  color: var(--dm-color-zinc-900);
  font-weight: 680;
}

.source-tabs button.active::after {
  background: var(--dm-color-zinc-900);
}

.source-tabs button span {
  padding: 1px 4px;
  margin-left: 3px;
  border-radius: 4px;
  background: var(--dm-color-zinc-100);
  font: 9px/1.3 var(--dm-font-mono);
}

.source-control-actions {
  gap: 7px;
}

.source-search {
  position: relative;
  display: flex;
  align-items: center;
  width: 210px;
  min-height: 31px;
  padding: 0;
  border: 1px solid var(--dm-color-border);
  border-radius: 6px;
  background: var(--dm-color-zinc-50);
}

.source-search :deep(.app-icon) {
  position: absolute;
  left: 9px;
  width: 13px;
  height: 13px;
  color: var(--dm-color-zinc-400);
  pointer-events: none;
}

.source-search input {
  width: 100%;
  min-height: 31px;
  padding: 5px 8px 5px 29px;
  border: 0;
  outline: 0;
  color: var(--dm-color-zinc-900);
  background: transparent;
  font-size: 10px;
}

.source-search:focus-within {
  border-color: #a5b4fc;
  background: #fff;
  box-shadow: 0 0 0 3px rgb(99 102 241 / 7%);
}

.source-refresh {
  display: grid;
  place-items: center;
  width: 31px;
  height: 31px;
  padding: 0;
  color: var(--dm-color-zinc-500);
  border: 1px solid var(--dm-color-border);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.source-refresh:hover:not(:disabled) {
  color: var(--dm-color-zinc-900);
  background: var(--dm-color-zinc-50);
}

.source-refresh:disabled {
  opacity: 0.5;
  cursor: wait;
}

.source-refresh :deep(.app-icon) {
  width: 14px;
  height: 14px;
}

.source-list-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
  padding: 6px 14px;
  color: var(--dm-color-zinc-500);
  border-bottom: 1px solid var(--dm-color-border);
  background: var(--dm-color-zinc-50);
  font-size: 9px;
}

.source-list-actions span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-table-head,
.source-row {
  display: grid;
  grid-template-columns: minmax(250px, 2.2fr) minmax(88px, 0.7fr) minmax(125px, 0.8fr) minmax(
      94px,
      0.6fr
    );
  align-items: center;
  gap: 12px;
}

.source-table-head {
  min-height: 32px;
  padding: 0 14px;
  color: var(--dm-color-zinc-500);
  border-bottom: 1px solid var(--dm-color-border);
  background: var(--dm-color-zinc-50);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.source-list-viewport {
  min-height: 0;
  overflow: auto;
}

.source-list {
  padding: 0;
  margin: 0;
  list-style: none;
}

.source-list li + li {
  border-top: 1px solid var(--dm-color-zinc-100);
}

.source-row {
  position: relative;
  width: 100%;
  min-height: 61px;
  padding: 8px 14px;
  text-align: left;
  border: 0;
  background: #fff;
  cursor: pointer;
  transition: background var(--dm-motion-fast) var(--dm-motion-easing);
}

.source-row::after {
  position: absolute;
  inset: 0 auto 0 0;
  width: 2px;
  background: transparent;
  content: '';
}

.source-row:hover {
  background: var(--dm-color-zinc-50);
}

.source-row--selected {
  background: rgb(238 242 255 / 50%);
}

.source-row--selected::after {
  background: var(--dm-color-brand);
}

.source-file-copy {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.file-glyph {
  display: grid;
  place-items: center;
  width: 29px;
  height: 29px;
  flex: 0 0 auto;
  color: var(--dm-color-zinc-500);
  border: 1px solid var(--dm-color-border);
  border-radius: 6px;
  background: var(--dm-color-zinc-50);
}

.file-glyph :deep(.app-icon) {
  width: 14px;
  height: 14px;
}

.source-name {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.source-name strong,
.source-name small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-name strong {
  color: var(--dm-color-zinc-900);
  font-size: 11px;
  font-weight: 650;
}

.source-name small,
.source-updated-at {
  color: var(--dm-color-zinc-400);
  font-size: 9px;
}

.source-row > code {
  overflow: hidden;
  color: var(--dm-color-zinc-600);
  font: 9px/1.4 var(--dm-font-mono);
  text-overflow: ellipsis;
}

.source-register-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 45px;
  padding: 7px 14px;
  border-top: 1px solid var(--dm-color-border);
  background: #fff;
}

.source-list-summary {
  display: flex;
  gap: 9px;
  color: var(--dm-color-zinc-500);
  font-size: 9px;
}

.text-action {
  padding: 0;
  color: var(--dm-color-brand);
  border: 0;
  background: transparent;
  font-size: 9px;
  font-weight: 650;
  cursor: pointer;
}

.source-pagination {
  display: flex;
  gap: 3px;
}

.source-pagination button {
  min-width: 27px;
  height: 27px;
  padding: 0 7px;
  color: var(--dm-color-zinc-500);
  border: 1px solid var(--dm-color-border);
  border-radius: 5px;
  background: #fff;
  font-size: 9px;
  cursor: pointer;
}

.source-pagination button:hover:not(:disabled),
.source-pagination button.active {
  color: var(--dm-color-zinc-900);
  border-color: var(--dm-color-zinc-300);
  background: var(--dm-color-zinc-100);
}

.source-pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.source-task-panel {
  position: sticky;
  top: 78px;
  display: grid;
  align-content: start;
  padding: 0;
}

.source-task-panel > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 55px;
  padding: 11px 13px;
  border-bottom: 1px solid var(--dm-color-border);
  background: var(--dm-color-zinc-50);
}

.eyebrow {
  margin: 0 0 3px;
  color: var(--dm-color-zinc-400);
  font: 700 8px/1.2 var(--dm-font-mono);
  letter-spacing: 0.1em;
}

.source-task-panel h2 {
  margin: 0;
  color: var(--dm-color-zinc-900);
  font-size: 12px;
}

.source-task-identity {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px;
  border-bottom: 1px solid var(--dm-color-border);
}

.source-task-identity > div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.source-task-identity strong,
.source-task-identity small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-task-identity strong {
  font-size: 11px;
}

.source-task-identity small {
  color: var(--dm-color-zinc-400);
  font-size: 9px;
}

.source-task-section {
  display: grid;
  gap: 9px;
  padding: 12px 13px;
  border-bottom: 1px solid var(--dm-color-zinc-100);
}

.source-task-section h3 {
  margin: 0;
  color: var(--dm-color-zinc-500);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.source-task-section dl {
  display: grid;
  gap: 7px;
  margin: 0;
}

.source-task-section dl > div,
.source-task-config > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.source-task-section dt,
.source-task-section dd,
.source-task-config span,
.source-task-config strong {
  margin: 0;
  color: var(--dm-color-zinc-500);
  font-size: 9px;
}

.source-task-section dd,
.source-task-config strong {
  color: var(--dm-color-zinc-800);
  font-weight: 600;
  text-align: right;
}

.source-task-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 0;
  margin: 0;
  list-style: none;
}

.source-task-steps li {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 5px;
  color: var(--dm-color-zinc-400);
}

.source-task-steps li::before {
  position: absolute;
  top: 8px;
  right: 50%;
  left: -50%;
  height: 1px;
  background: var(--dm-color-border);
  content: '';
}

.source-task-steps li:first-child::before {
  display: none;
}

.source-task-steps span {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 17px;
  height: 17px;
  border: 1px solid var(--dm-color-zinc-300);
  border-radius: 50%;
  background: #fff;
  font: 7px/1 var(--dm-font-mono);
}

.source-task-steps strong {
  font-size: 8px;
  font-weight: 600;
  text-align: center;
}

.source-task-steps .is-complete {
  color: var(--dm-color-success);
}

.source-task-steps .is-current {
  color: var(--dm-color-brand);
}

.source-task-panel > footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px;
  padding: 13px;
}

.source-task-panel > footer p {
  grid-column: 1 / -1;
  margin: 0 0 3px;
  color: var(--dm-color-zinc-500);
  font-size: 9px;
  line-height: 1.55;
}

.source-task-panel--empty {
  place-items: center;
  min-height: 180px;
  padding: 25px;
  color: var(--dm-color-zinc-500);
  text-align: center;
}

.source-task-panel--empty strong {
  color: var(--dm-color-zinc-800);
  font-size: 11px;
}

.source-task-panel--empty span {
  font-size: 9px;
}

.skeleton-list {
  display: grid;
  gap: 1px;
}

.skeleton-list i {
  min-height: 60px;
  background: linear-gradient(100deg, #fafafa 20%, #f4f4f5 42%, #fafafa 64%);
  background-size: 220% 100%;
  animation: source-loading 1.5s linear infinite;
}

.source-no-results,
.empty-register {
  display: grid;
  place-items: center;
  align-content: center;
  min-height: 300px;
  padding: 24px;
  text-align: center;
}

.source-no-results strong,
.empty-register strong {
  margin-top: 7px;
  font-size: 12px;
}

.source-no-results p,
.empty-register p {
  margin: 4px 0 9px;
  color: var(--dm-color-zinc-500);
  font-size: 10px;
}

.upload-modal-backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgb(24 24 27 / 38%);
  backdrop-filter: blur(3px);
}

.upload-panel--modal {
  display: grid;
  gap: 14px;
  width: min(100%, 470px);
  padding: 0 17px 17px;
  overflow: hidden;
  border: 1px solid var(--dm-color-border);
  border-radius: var(--dm-radius-large);
  background: #fff;
  box-shadow: 0 24px 70px rgb(24 24 27 / 22%);
  animation: source-modal-reveal 220ms var(--dm-motion-easing) both;
}

.upload-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  margin: 0 -17px;
  padding: 10px 17px;
  border-bottom: 1px solid var(--dm-color-border);
  background: var(--dm-color-zinc-50);
}

.upload-modal-header h2 {
  margin: 2px 0 0;
  font-size: 14px;
}

.section-index {
  margin: 0;
  color: var(--dm-color-zinc-400);
  font: 700 8px/1 var(--dm-font-mono);
  letter-spacing: 0.1em;
}

.upload-modal-header > div:last-child {
  display: flex;
  align-items: center;
  gap: 7px;
}

.upload-modal-close {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--dm-color-zinc-500);
  border: 1px solid var(--dm-color-border);
  border-radius: 6px;
  background: #fff;
  font-size: 17px;
  cursor: pointer;
}

.drop-zone {
  position: relative;
  display: grid;
  place-items: center;
  gap: 6px;
  min-height: 150px;
  padding: 22px;
  text-align: center;
  border: 1px dashed var(--dm-color-zinc-300);
  border-radius: var(--dm-radius-medium);
  background: var(--dm-color-zinc-50);
  transition:
    border var(--dm-motion-fast) var(--dm-motion-easing),
    background var(--dm-motion-fast) var(--dm-motion-easing);
}

.drop-zone--active,
.drop-zone--selected {
  border-color: #a5b4fc;
  background: var(--dm-color-brand-soft);
}

.drop-zone input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.drop-zone :deep(.app-icon) {
  width: 24px;
  height: 24px;
  color: var(--dm-color-brand);
}

.drop-zone strong {
  font-size: 11px;
}

.drop-zone span {
  color: var(--dm-color-zinc-500);
  font-size: 9px;
}

.upload-progress {
  height: 4px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--dm-color-zinc-100);
}

.upload-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--dm-color-brand);
  transition: width var(--dm-motion-normal) var(--dm-motion-easing);
}

@keyframes source-loading {
  to {
    background-position: -220% 0;
  }
}

@keyframes source-modal-reveal {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.985);
  }
}

@media (max-width: 1050px) {
  .source-workbench {
    grid-template-columns: 1fr;
  }

  .source-task-panel {
    position: static;
  }
}

@media (max-width: 720px) {
  .source-page-heading {
    padding: 10px 14px;
  }

  .source-workbench {
    padding: 14px;
  }

  .source-register {
    min-height: 520px;
  }

  .source-list-controls {
    align-items: stretch;
    flex-direction: column;
    padding: 0 12px 10px;
  }

  .source-tabs {
    min-height: 44px;
  }

  .source-control-actions,
  .source-search {
    width: 100%;
  }

  .source-table-head,
  .source-row {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .source-table-head span:nth-child(3),
  .source-table-head span:nth-child(4),
  .source-updated-at,
  .source-row > code {
    display: none;
  }

  .source-register-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .source-list-actions span:last-child {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-list i,
  .upload-panel--modal {
    animation: none;
  }
}
</style>
