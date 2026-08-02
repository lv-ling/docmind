<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';

import AppIcon from '@/components/AppIcon.vue';
import InlineNotice from '@/components/InlineNotice.vue';
import { DmButton, DmFileInput, DmStatus, DmTextField } from '@/ui';
import { formatBytes } from '@/utils/file.js';

import type { UploadStage } from '../composables/useSourceUpload.js';

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

const dialogRef = ref<HTMLElement | null>(null);
const fileInputRef = ref<InstanceType<typeof DmFileInput> | null>(null);
const isDragActive = ref(false);
let previousFocusElement: HTMLElement | null = null;

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

const handleDialogKeydown = (event: KeyboardEvent): void => {
  if (event.key === 'Escape') {
    handleClose();
    return;
  }
  if (event.key !== 'Tab' || dialogRef.value === null) return;
  const focusableElements = Array.from(
    dialogRef.value.querySelectorAll<HTMLElement>(
      'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])',
    ),
  );
  const firstElement = focusableElements[0];
  const lastElement = focusableElements.at(-1);
  if (firstElement === undefined || lastElement === undefined) return;
  if (event.shiftKey && document.activeElement === firstElement) {
    event.preventDefault();
    lastElement.focus();
  } else if (!event.shiftKey && document.activeElement === lastElement) {
    event.preventDefault();
    firstElement.focus();
  }
};

watch(
  () => props.isOpen,
  async (isOpen) => {
    if (isOpen) {
      previousFocusElement =
        document.activeElement instanceof HTMLElement ? document.activeElement : null;
      await nextTick();
      dialogRef.value?.focus();
    } else {
      await nextTick();
      previousFocusElement?.focus();
      previousFocusElement = null;
    }
  },
);

watch(
  () => props.selectedFile,
  (selectedFile) => {
    if (selectedFile === null) fileInputRef.value?.reset();
  },
);
</script>

<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      class="upload-modal-backdrop"
      role="presentation"
      @mousedown.self="handleClose"
    >
      <section
        ref="dialogRef"
        class="upload-panel upload-panel--modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="upload-title"
        tabindex="-1"
        @keydown="handleDialogKeydown"
      >
        <header class="upload-modal-header">
          <div>
            <p class="section-index">NEW SOURCE</p>
            <h2 id="upload-title">上传文档</h2>
          </div>
          <div>
            <DmStatus
              :label="uploadStageLabel"
              :tone="uploadStage === 'done' ? 'success' : isUploading ? 'info' : 'neutral'"
              live
            />
            <DmButton variant="secondary" icon-only aria-label="关闭上传窗口" @click="handleClose">
              <AppIcon name="close" />
            </DmButton>
          </div>
        </header>
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
          <DmFileInput
            ref="fileInputRef"
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
        <div v-if="isUploading" class="upload-progress" aria-live="polite">
          <span
            :style="{
              width: `${uploadStage === 'uploading' ? uploadProgress : uploadStage === 'hashing' ? 12 : 100}%`,
            }"
          ></span>
        </div>
        <InlineNotice v-if="uploadError" tone="danger" title="上传未完成" :detail="uploadError" />
        <DmButton
          :disabled="selectedFile === null || documentName.trim().length === 0"
          :loading="isUploading"
          @click="emit('submit')"
        >
          上传并校验
        </DmButton>
      </section>
    </div>
  </Teleport>
</template>
