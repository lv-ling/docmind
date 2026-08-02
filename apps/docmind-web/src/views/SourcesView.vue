<script setup lang="ts">
import type { SourceDocument, SourceVersionId, WorkspaceId } from '@/contracts';
import { DmButton, DmStatus, DmTextField } from '@/ui';
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

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

watch([searchQuery, sourceFilter], () => {
  currentPage.value = 1;
  const nextSources = filteredSources.value;
  if (!nextSources.some((source) => source.id === selectedSourceId.value)) {
    selectedSourceId.value = nextSources[0]?.id ?? null;
  }
});

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
        <p class="eyebrow">SOURCE REGISTER / W2</p>
        <h1>原始文档</h1>
        <p>不可变原件、版本与处理状态集中管理。</p>
      </div>
    </header>

    <div class="source-workbench">
      <section class="source-register" aria-label="文档登记簿">
        <div class="source-list-controls">
          <label class="source-search">
            <span class="dm-sr-only">搜索文档</span>
            <input v-model="searchQuery" type="search" placeholder="搜索文档名称" />
          </label>
          <label class="source-filter">
            <span class="dm-sr-only">文档状态</span>
            <select v-model="sourceFilter">
              <option value="all">全部状态</option>
              <option value="registered">已登记</option>
              <option value="pending">待上传</option>
            </select>
          </label>
          <button class="source-refresh" type="button" :disabled="loading" @click="load">
            刷新列表
          </button>
        </div>

        <div class="source-list-actions">
          <span v-if="selectedSource">已选择：{{ selectedSource.name }}</span>
          <span v-else>选择文档后可查看任务信息</span>
          <DmButton type="button" size="small" @click="uploadModalOpen = true">
            <AppIcon name="upload" /><span>上传文档</span>
          </DmButton>
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
            <p class="eyebrow">CURRENT TASK</p>
            <h2>当前任务</h2>
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
              <p class="section-index">新建原始文档</p>
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
