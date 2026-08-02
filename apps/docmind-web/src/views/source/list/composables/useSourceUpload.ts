import type { SourceVersionId, WorkspaceId } from '@/contracts';
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { uploadFileDirectly } from '@/api/client.js';
import { completeSourceUpload, createSourceUpload } from '@/api/sources.js';
import { useWorkspaceStore } from '@/stores/workspace.js';
import { sha256Hex, validateSourceFile } from '@/utils/file.js';

export type UploadStage = 'idle' | 'hashing' | 'uploading' | 'verifying' | 'done';

export const useSourceUpload = (handleUploaded: () => Promise<void>) => {
  const route = useRoute();
  const router = useRouter();
  const workspace = useWorkspaceStore();
  const workspaceId = computed(() => workspace.selectedId as WorkspaceId);
  const selectedFile = ref<File | null>(null);
  const documentName = ref('');
  const uploadStage = ref<UploadStage>('idle');
  const uploadProgress = ref(0);
  const uploadError = ref('');
  const isUploadDialogOpen = ref(false);

  const isUploading = computed(() => !['idle', 'done'].includes(uploadStage.value));
  const uploadStageLabel = computed(() => {
    const labels: Record<UploadStage, string> = {
      idle: '等待上传',
      hashing: '正在计算文件指纹',
      uploading: `正在上传 ${uploadProgress.value}%`,
      verifying: '服务端正在校验文件',
      done: '上传完成',
    };
    return labels[uploadStage.value];
  });

  const openUploadDialog = (): void => {
    isUploadDialogOpen.value = true;
  };

  const closeUploadDialog = (): void => {
    if (!isUploading.value) isUploadDialogOpen.value = false;
  };

  const selectUploadFile = (file: File): void => {
    uploadError.value = '';
    try {
      validateSourceFile(file);
      selectedFile.value = file;
      documentName.value = file.name.replace(/\.(docx?|pdf)$/i, '');
      uploadStage.value = 'idle';
      uploadProgress.value = 0;
    } catch (caught) {
      selectedFile.value = null;
      uploadError.value = caught instanceof Error ? caught.message : '文件不符合要求';
    }
  };

  const uploadSource = async (): Promise<void> => {
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
      if (session.upload.upload_url === null) {
        throw new Error('上传会话已失效，请重新选择文件');
      }

      uploadStage.value = 'uploading';
      const uploaded = await uploadFileDirectly(
        session.upload.upload_url,
        file,
        session.upload.required_headers,
        (state) => (uploadProgress.value = state.percentage),
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
      await handleUploaded();
      isUploadDialogOpen.value = false;
    } catch (caught) {
      uploadStage.value = 'idle';
      uploadError.value = caught instanceof Error ? caught.message : '上传失败，请重试';
    }
  };

  watch(
    () => route.query.upload,
    async (value) => {
      if (value !== '1') return;
      openUploadDialog();
      const nextQuery = { ...route.query };
      delete nextQuery.upload;
      await router.replace({ query: nextQuery });
    },
    { immediate: true },
  );

  return {
    selectedFile,
    documentName,
    uploadStage,
    uploadProgress,
    uploadError,
    isUploadDialogOpen,
    isUploading,
    uploadStageLabel,
    openUploadDialog,
    closeUploadDialog,
    selectUploadFile,
    uploadSource,
  };
};
