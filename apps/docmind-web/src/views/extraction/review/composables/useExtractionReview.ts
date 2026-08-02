import type {
  ExtractionFieldResultView,
  ExtractionRunId,
  ExtractionRunStatus,
  JsonValue,
  SourcePreviewAccess,
} from '@/contracts';
import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue';
import { useRoute } from 'vue-router';

import { getAuthenticatedObjectUrl } from '@/api/client.js';
import {
  approveExtraction,
  connectExtractionEvents,
  getExtraction,
  reviewExtractionField,
} from '@/api/extractions.js';
import { getSourcePreview } from '@/api/sources.js';
import { getQueryString } from '@/router/query.js';
import { useAuthStore } from '@/stores/auth.js';

const TERMINAL_STATUSES: ExtractionRunStatus[] = ['review_required', 'approved', 'failed'];

export const useExtractionReview = () => {
  const route = useRoute();
  const auth = useAuthStore();
  const extractionId = computed(() => getQueryString(route.query.extractionId) as ExtractionRunId);
  const run = shallowRef<Awaited<ReturnType<typeof getExtraction>> | null>(null);
  const preview = shallowRef<SourcePreviewAccess | null>(null);
  const previewObjectUrl = ref<string | null>(null);
  const isLoadingReview = ref(true);
  const reviewError = ref('');
  const connectionNotice = ref('');
  const isApprovingExtraction = ref(false);
  const savingFieldId = ref<string | null>(null);
  const editingFieldId = ref<string | null>(null);
  const editedValue = ref('');
  const selectedFieldId = ref<string | null>(null);
  const splitPercentage = ref(43);
  const isLeftPanelCollapsed = ref(false);
  const isRightPanelCollapsed = ref(false);
  let stopEvents: (() => void) | null = null;
  let pollTimer: ReturnType<typeof setTimeout> | null = null;
  let previewTimer: ReturnType<typeof setTimeout> | null = null;

  const fields = computed<ExtractionFieldResultView[]>(() => run.value?.result?.fields ?? []);
  const selectedField = computed<ExtractionFieldResultView | null>(
    () =>
      fields.value.find((field) => field.id === selectedFieldId.value) ?? fields.value[0] ?? null,
  );
  const reviewProgress = computed(() => {
    if (fields.value.length === 0) return 0;
    return Math.round(
      (fields.value.filter((field) => field.review_status !== 'pending').length /
        fields.value.length) *
        100,
    );
  });
  const canApprove = computed(
    () =>
      run.value?.status === 'review_required' &&
      fields.value.every((field) => field.review_status !== 'pending'),
  );

  const loadPreview = async (): Promise<void> => {
    if (run.value === null) return;
    try {
      preview.value = await getSourcePreview(run.value.source_version_id);
      if (
        preview.value.preview.status === 'ready' &&
        preview.value.view_url !== null &&
        previewObjectUrl.value === null
      ) {
        previewObjectUrl.value = await getAuthenticatedObjectUrl(preview.value.view_url);
      } else if (['queued', 'processing'].includes(preview.value.preview.status)) {
        previewTimer = setTimeout(loadPreview, 2500);
      }
    } catch {
      preview.value = null;
    }
  };

  const loadExtractionReview = async (isSilent = false): Promise<void> => {
    if (!isSilent) isLoadingReview.value = true;
    try {
      run.value = await getExtraction(extractionId.value);
      if (selectedFieldId.value === null) {
        selectedFieldId.value = run.value.result?.fields[0]?.id ?? null;
      }
      if (preview.value === null) await loadPreview();
    } catch (caught) {
      reviewError.value = caught instanceof Error ? caught.message : '抽取结果加载失败';
    } finally {
      isLoadingReview.value = false;
    }
  };

  const startPolling = (): void => {
    if (pollTimer !== null) return;
    const poll = async (): Promise<void> => {
      await loadExtractionReview(true);
      if (run.value !== null && !TERMINAL_STATUSES.includes(run.value.status)) {
        pollTimer = setTimeout(poll, 2200);
      } else {
        pollTimer = null;
      }
    };
    pollTimer = setTimeout(poll, 500);
  };

  const startEvents = (): void => {
    if (auth.accessToken === null) return;
    stopEvents = connectExtractionEvents(
      extractionId.value,
      auth.accessToken,
      () => void loadExtractionReview(true),
      () => {
        connectionNotice.value = '实时通道暂不可用，已自动切换为状态轮询。';
        startPolling();
      },
    );
  };

  const submitFieldReview = async (
    field: ExtractionFieldResultView,
    action: 'accept' | 'modify' | 'reject',
  ): Promise<void> => {
    savingFieldId.value = field.id;
    reviewError.value = '';
    try {
      let value: JsonValue | null = null;
      if (action === 'modify') {
        try {
          value = JSON.parse(editedValue.value) as JsonValue;
        } catch {
          value = editedValue.value;
        }
      }
      run.value = await reviewExtractionField(extractionId.value, field.id, {
        action,
        value,
        reason: action === 'reject' ? '复核人确认该字段不应采用' : null,
      });
      editingFieldId.value = null;
    } catch (caught) {
      reviewError.value = caught instanceof Error ? caught.message : '字段复核保存失败';
    } finally {
      savingFieldId.value = null;
    }
  };

  const beginFieldModification = (field: ExtractionFieldResultView): void => {
    editingFieldId.value = field.id;
    editedValue.value =
      field.display_value.access === 'visible' && field.display_value.value !== null
        ? typeof field.display_value.value === 'string'
          ? field.display_value.value
          : JSON.stringify(field.display_value.value)
        : '';
  };

  const approveExtractionResult = async (): Promise<void> => {
    isApprovingExtraction.value = true;
    try {
      run.value = await approveExtraction(extractionId.value, { note: 'Web 工作台复核完成' });
    } catch (caught) {
      reviewError.value = caught instanceof Error ? caught.message : '批准失败';
    } finally {
      isApprovingExtraction.value = false;
    }
  };

  const selectField = (fieldId: string): void => {
    selectedFieldId.value = fieldId;
  };

  onMounted(async () => {
    await loadExtractionReview();
    if (run.value !== null && !TERMINAL_STATUSES.includes(run.value.status)) startEvents();
  });

  onUnmounted(() => {
    stopEvents?.();
    if (pollTimer !== null) clearTimeout(pollTimer);
    if (previewTimer !== null) clearTimeout(previewTimer);
    if (previewObjectUrl.value !== null) URL.revokeObjectURL(previewObjectUrl.value);
  });

  return {
    extractionId,
    run,
    previewObjectUrl,
    isLoadingReview,
    reviewError,
    connectionNotice,
    isApprovingExtraction,
    savingFieldId,
    editingFieldId,
    editedValue,
    splitPercentage,
    isLeftPanelCollapsed,
    isRightPanelCollapsed,
    fields,
    selectedField,
    reviewProgress,
    canApprove,
    submitFieldReview,
    beginFieldModification,
    approveExtractionResult,
    selectField,
  };
};
