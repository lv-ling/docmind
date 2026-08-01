<script setup lang="ts">
import type { SourceDocument, SourceVersionId, WorkspaceId } from '@docmind/contracts';
import { DmButton, DmStatus, DmTextField } from '@docmind/ui';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { uploadFileDirectly } from '../api/client.js';
import { completeSourceUpload, createSourceUpload, listSources } from '../api/sources.js';
import AppIcon from '../components/AppIcon.vue';
import InlineNotice from '../components/InlineNotice.vue';
import { formatBytes, sha256Hex, validateSourceFile } from '../utils/file.js';

type UploadStage = 'idle' | 'hashing' | 'uploading' | 'verifying' | 'done';

const route = useRoute();
const router = useRouter();
const workspaceId = computed(() => route.params.workspaceId as WorkspaceId);
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

const load = async (): Promise<void> => {
  loading.value = true;
  loadError.value = '';
  try {
    const page = await listSources(workspaceId.value);
    sources.value = page.items;
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '文档列表加载失败';
  } finally {
    loading.value = false;
  }
};

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
  } catch (error) {
    uploadStage.value = 'idle';
    uploadError.value = error instanceof Error ? error.message : '上传失败，请重试';
  }
};

onMounted(load);
</script>

<template>
  <section class="page-stack">
    <header class="page-heading">
      <div>
        <p class="eyebrow">SOURCE REGISTER / W2</p>
        <h1>原始文档</h1>
        <p>不可变原件、版本与处理状态集中管理。</p>
      </div>
      <span class="record-count">{{ sources.length.toString().padStart(2, '0') }} 份记录</span>
    </header>

    <div class="source-layout">
      <section class="upload-panel" aria-labelledby="upload-title">
        <div class="section-heading">
          <div>
            <span>01</span>
            <h2 id="upload-title">上传新文档</h2>
          </div>
          <DmStatus
            :label="stageLabel"
            :tone="uploadStage === 'done' ? 'success' : uploading ? 'info' : 'neutral'"
            live
          />
        </div>
        <div
          class="drop-zone"
          :class="{ 'drop-zone--active': dragActive, 'drop-zone--selected': selectedFile !== null }"
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

      <section class="source-register" aria-labelledby="source-list-title">
        <div class="section-heading section-heading--bordered">
          <div>
            <span>02</span>
            <h2 id="source-list-title">文档登记簿</h2>
          </div>
          <button class="text-action" type="button" :disabled="loading" @click="load">刷新</button>
        </div>
        <InlineNotice v-if="loadError" tone="danger" title="列表加载失败" :detail="loadError" />
        <div v-if="loading" class="skeleton-list" aria-label="正在加载文档">
          <i v-for="index in 4" :key="index"></i>
        </div>
        <div v-else-if="sources.length === 0" class="empty-register">
          <span>00</span><strong>还没有原始文档</strong>
          <p>上传第一份文档后，将在这里显示版本与处理入口。</p>
        </div>
        <ol v-else class="source-list">
          <li v-for="(source, index) in sources" :key="source.id">
            <button type="button" @click="router.push(`/w/${workspaceId}/sources/${source.id}`)">
              <span class="source-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="file-glyph"><AppIcon name="document" /></span>
              <span class="source-name"
                ><strong>{{ source.name }}</strong
                ><small
                  >更新于 {{ new Date(source.updated_at).toLocaleString('zh-CN') }}</small
                ></span
              >
              <DmStatus
                :label="source.current_version_id === null ? '待上传' : '已登记'"
                :tone="source.current_version_id === null ? 'warning' : 'success'"
              />
              <AppIcon name="arrow" />
            </button>
          </li>
        </ol>
      </section>
    </div>
  </section>
</template>
