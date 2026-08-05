<script setup lang="ts">
import { DmButton, DmDialog, DmProgress, DmStatus, DmTextField } from '@/ui';
import { computed, ref, watch } from 'vue';

import { AppIcon, InlineNotice } from '@/components/index.js';
import { formatBytes } from '@/utils/file.js';

import type { UploadStage } from '../../composables/useSourceUpload.js';

defineOptions({ name: 'SourceUploadDialog' });

const props = defineProps<{
  isOpen: boolean;
  selectedFile: File | null;
  uploadStage: UploadStage;
  uploadProgress: number;
  uploadError: string;
  isUploading: boolean;
  uploadStageLabel: string;
}>();

const documentName = defineModel<string>('documentName', { required: true });

const emit = defineEmits<{
  close: [];
  'select-file': [file: File];
  submit: [];
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
const isDragActive = ref(false);
const progressValue = computed(() => {
  if (props.uploadStage === 'uploading') return props.uploadProgress;
  if (props.uploadStage === 'hashing') return 12;
  return 100;
});

const handleClose = (): void => {
  if (!props.isUploading) emit('close');
};

const handleFileInput = (event: Event): void => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (file !== undefined) emit('select-file', file);
};

const handleFileDrop = (event: DragEvent): void => {
  isDragActive.value = false;
  const file = event.dataTransfer?.files[0];
  if (file !== undefined) emit('select-file', file);
};

watch(
  () => props.selectedFile,
  (selectedFile) => {
    if (selectedFile === null && fileInputRef.value !== null) fileInputRef.value.value = '';
  },
);
</script>

<template>
  <DmDialog
    :open="isOpen"
    title="上传文档"
    description="上传 DOC、DOCX 或 PDF，系统会依次完成摘要计算、安全校验和预览生成。"
    :close-on-backdrop="!isUploading"
    :close-on-escape="!isUploading"
    @close="handleClose"
  >
    <div class="upload-dialog-content">
      <DmStatus
        :label="uploadStageLabel"
        :tone="uploadStage === 'done' ? 'success' : isUploading ? 'info' : 'neutral'"
        live
      />
      <div
        class="drop-zone"
        :class="{
          'drop-zone--active': isDragActive,
          'drop-zone--selected': selectedFile !== null,
        }"
        @dragenter.prevent="isDragActive = true"
        @dragover.prevent
        @dragleave.prevent="isDragActive = false"
        @drop.prevent="handleFileDrop"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept=".doc,.docx,.pdf"
          aria-label="选择 DOC、DOCX 或 PDF 文件"
          @change="handleFileInput"
        />
        <AppIcon name="upload" />
        <template v-if="selectedFile === null">
          <strong>拖入文档，或点击浏览</strong><span>DOC / DOCX / PDF · 最大 10 MB</span>
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
        :disabled="isUploading"
        required
      />
      <DmProgress
        v-if="isUploading"
        :value="progressValue"
        :label="uploadStageLabel"
        aria-live="polite"
      />
      <InlineNotice v-if="uploadError" tone="danger" title="上传未完成" :detail="uploadError" />
    </div>
    <template #footer>
      <DmButton variant="secondary" :disabled="isUploading" @click="handleClose">取消</DmButton>
      <DmButton
        :disabled="selectedFile === null || documentName.trim().length === 0"
        :loading="isUploading"
        @click="emit('submit')"
      >
        上传并校验
      </DmButton>
    </template>
  </DmDialog>
</template>
