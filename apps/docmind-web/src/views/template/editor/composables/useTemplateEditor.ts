import type {
  SourcePreviewAccess,
  TemplateDetail,
  TemplateId,
  TemplateVersion,
  TemplateVersionId,
} from '@/contracts';
import { assertValidControlledDocument, type ControlledDocument } from '@/editor';
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getSourcePreview } from '@/api/sources.js';
import { getAuthenticatedObjectUrl, getTemplate } from '@/api/templates.js';
import { RouteName } from '@/router/constants.js';
import { getQueryString } from '@/router/query.js';
import { cloneJsonValue } from '@/utils/json.js';

import { injectTemplateResourceUrls } from '../model/template-document.js';
import { useTemplateDocumentEditor } from './useTemplateDocumentEditor.js';
import { useTemplateNativeEditor } from './useTemplateNativeEditor.js';
import { useTemplateVersionActions } from './useTemplateVersionActions.js';
import { useTemplateWorkspaceLayout } from './useTemplateWorkspaceLayout.js';

export const useTemplateEditor = () => {
  const route = useRoute();
  const router = useRouter();
  const templateId = computed(() => getQueryString(route.query.templateId) as TemplateId);
  const detail = ref<TemplateDetail | null>(null);
  const selectedVersionId = ref<TemplateVersionId | null>(null);
  const draft = ref<ControlledDocument | null>(null);
  const serverHtml = ref('');
  const isLoadingTemplate = ref(true);
  const editorError = ref('');
  const editorNotice = ref('');
  const changeSummary = ref('调整模板文字与版式');
  const originalPreview = ref<SourcePreviewAccess | null>(null);
  const originalObjectUrl = ref<string | null>(null);
  const resourceUrls = ref<string[]>([]);
  const previewFrameRef = ref<HTMLIFrameElement | null>(null);
  let refreshTimer: ReturnType<typeof setTimeout> | null = null;
  let previewTimer: ReturnType<typeof setTimeout> | null = null;

  const selectedVersion = computed<TemplateVersion | null>(
    () => detail.value?.versions.find((version) => version.id === selectedVersionId.value) ?? null,
  );
  const currentVersion = computed(() => detail.value?.current_version ?? null);
  const isCurrentVersion = computed(
    () => selectedVersionId.value !== null && selectedVersionId.value === currentVersion.value?.id,
  );
  const blockingWarnings = computed(
    () => selectedVersion.value?.warnings.filter((warning) => warning.blocking) ?? [],
  );

  const {
    splitPercentage,
    isLeftPanelCollapsed,
    isRightPanelCollapsed,
    zoomPercentage,
    originalPage,
  } = useTemplateWorkspaceLayout();

  const pageCount = computed(
    () => originalPreview.value?.preview.page_count ?? draft.value?.metadata.source_page_count ?? 1,
  );
  const originalSrc = computed(() =>
    originalObjectUrl.value === null
      ? null
      : `${originalObjectUrl.value}#page=${originalPage.value}&zoom=page-width`,
  );

  const documentEditor = useTemplateDocumentEditor({
    draft,
    selectedVersion,
    serverHtml,
    resourceUrls,
    zoomPercentage,
    editorNotice,
  });
  const nativeEditor = useTemplateNativeEditor({
    templateId,
    isCurrentVersion,
    isEditMode: documentEditor.isEditMode,
    editorError,
    editorNotice,
  });

  const revokeResources = (): void => {
    resourceUrls.value.forEach((url) => URL.revokeObjectURL(url));
    resourceUrls.value = [];
  };

  const prepareVersion = async (version: TemplateVersion): Promise<void> => {
    if (nativeEditor.isNativeMode.value) nativeEditor.stopNativeEditor();
    selectedVersionId.value = version.id;
    try {
      const model: unknown = cloneJsonValue(version.document_model);
      assertValidControlledDocument(model);
      draft.value = model;
      documentEditor.selectedNodeId.value = documentEditor.editableBlocks.value[0]?.id ?? null;
      documentEditor.hasUnsavedChanges.value = false;
      documentEditor.isEditMode.value = false;
      revokeResources();
      resourceUrls.value = await Promise.all(
        version.resources.map((resource) => getAuthenticatedObjectUrl(resource.download_url)),
      );
      serverHtml.value = injectTemplateResourceUrls(
        version.document.html,
        version.resources,
        resourceUrls.value,
      );
    } catch (caught) {
      editorError.value = caught instanceof Error ? caught.message : '受控文档模型校验失败';
      draft.value = null;
    }
  };

  const loadOriginalPreview = async (): Promise<void> => {
    if (detail.value === null || originalObjectUrl.value !== null) return;
    try {
      originalPreview.value = await getSourcePreview(detail.value.template.source_version_id);
      if (
        originalPreview.value.preview.status === 'ready' &&
        originalPreview.value.view_url !== null
      ) {
        originalObjectUrl.value = await getAuthenticatedObjectUrl(originalPreview.value.view_url);
      } else if (['queued', 'processing'].includes(originalPreview.value.preview.status)) {
        if (previewTimer !== null) clearTimeout(previewTimer);
        previewTimer = setTimeout(loadOriginalPreview, 2000);
      }
    } catch (caught) {
      editorError.value = caught instanceof Error ? caught.message : '原件预览读取失败';
    }
  };

  const loadTemplate = async (): Promise<void> => {
    editorError.value = '';
    try {
      const response = await getTemplate(templateId.value);
      const priorCurrent = detail.value?.current_version?.id ?? null;
      detail.value = response;
      if (response.current_version !== null && priorCurrent !== response.current_version.id) {
        await prepareVersion(response.current_version);
      }
      if (refreshTimer !== null) clearTimeout(refreshTimer);
      if (['queued', 'running', 'retrying'].includes(response.template.conversion_status)) {
        refreshTimer = setTimeout(loadTemplate, 2500);
      }
      if (response.template.conversion_status === 'ready') await loadOriginalPreview();
    } catch (caught) {
      editorError.value = caught instanceof Error ? caught.message : '模板读取失败';
    } finally {
      isLoadingTemplate.value = false;
    }
  };

  const versionActions = useTemplateVersionActions({
    templateId,
    draft,
    currentVersion,
    selectedVersion,
    isCurrentVersion,
    hasUnsavedChanges: documentEditor.hasUnsavedChanges,
    changeSummary,
    editorError,
    editorNotice,
    reloadTemplate: loadTemplate,
  });

  const focusNode = async (nodeId: string | null, page: number | null): Promise<void> => {
    if (
      nodeId !== null &&
      documentEditor.editableBlocks.value.some((block) => block.id === nodeId)
    ) {
      documentEditor.selectedNodeId.value = nodeId;
    }
    if (page !== null) originalPage.value = Math.max(1, Math.min(pageCount.value, page));
    await nextTick();
    if (nodeId === null) return;
    const selector = `[data-dm-node-id="${nodeId.replaceAll('"', '\\"')}"]`;
    previewFrameRef.value?.contentDocument?.querySelector(selector)?.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
    });
  };

  const showPreviewMode = (): void => {
    documentEditor.isEditMode.value = false;
    nativeEditor.stopNativeEditor();
  };
  const showEditMode = (): void => {
    nativeEditor.stopNativeEditor();
    documentEditor.isEditMode.value = true;
  };
  const openTemplateList = async (): Promise<void> => {
    await router.push({ name: RouteName.TemplateList });
  };

  onMounted(loadTemplate);
  onUnmounted(() => {
    if (refreshTimer !== null) clearTimeout(refreshTimer);
    if (previewTimer !== null) clearTimeout(previewTimer);
    if (originalObjectUrl.value !== null) URL.revokeObjectURL(originalObjectUrl.value);
    revokeResources();
  });

  return {
    detail,
    selectedVersionId,
    draft,
    isLoadingTemplate,
    editorError,
    editorNotice,
    changeSummary,
    selectedVersion,
    currentVersion,
    isCurrentVersion,
    blockingWarnings,
    pageCount,
    originalSrc,
    previewFrameRef,
    splitPercentage,
    isLeftPanelCollapsed,
    isRightPanelCollapsed,
    zoomPercentage,
    originalPage,
    ...documentEditor,
    ...nativeEditor,
    ...versionActions,
    prepareVersion,
    focusNode,
    showPreviewMode,
    showEditMode,
    openTemplateList,
  };
};
