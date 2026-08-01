<script setup lang="ts">
import type {
  JsonObject,
  SourcePreviewAccess,
  TemplateDetail,
  TemplateId,
  TemplateVersion,
  TemplateVersionId,
} from '@docmind/contracts';
import {
  assertValidControlledDocument,
  serializeControlledDocument,
  type ControlledDocument,
} from '@docmind/editor';
import { DmButton, DmSplitPane, DmStatus } from '@docmind/ui';
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getSourcePreview } from '../api/sources.js';
import {
  createNativeEditorSession,
  createTemplateVersion,
  getAuthenticatedObjectUrl,
  getNativeEditorSessionStatus,
  getTemplate,
  publishTemplateVersion,
  rollbackTemplate,
  type NativeEditorSession,
  type NativeEditorSessionStatus,
} from '../api/templates.js';
import InlineNotice from '../components/InlineNotice.vue';
import { useAuthStore } from '../stores/auth.js';
import { cloneJsonValue } from '../utils/json.js';

type MutableNode = Record<string, unknown> & { id?: unknown; type?: unknown };
interface EditableBlock {
  id: string;
  type: 'paragraph' | 'heading';
  text: string;
  page: number | null;
}
interface DiffChange {
  path: string;
  kind: 'added' | 'removed' | 'changed';
}

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const templateId = computed(() => String(route.params.templateId) as TemplateId);
const detail = ref<TemplateDetail | null>(null);
const selectedVersionId = ref<TemplateVersionId | null>(null);
const draft = ref<ControlledDocument | null>(null);
const selectedNodeId = ref<string | null>(null);
const serverHtml = ref('');
const loading = ref(true);
const saving = ref(false);
const publishing = ref(false);
const rollingBack = ref(false);
const error = ref('');
const notice = ref('');
const dirty = ref(false);
const editMode = ref(false);
const nativeMode = ref(false);
const nativeLoading = ref(false);
const nativeSession = ref<NativeEditorSession | null>(null);
const nativeStatus = ref<NativeEditorSessionStatus | null>(null);
const nativeEditorHost = ref<HTMLDivElement | null>(null);
const changeSummary = ref('调整模板文字与版式');
const split = ref(48);
const leftCollapsed = ref(false);
const rightCollapsed = ref(false);
const zoom = ref(90);
const originalPreview = ref<SourcePreviewAccess | null>(null);
const originalObjectUrl = ref<string | null>(null);
const originalPage = ref(1);
const rightFrame = ref<HTMLIFrameElement | null>(null);
const resourceUrls = ref<string[]>([]);
let refreshTimer: ReturnType<typeof setTimeout> | null = null;
let previewTimer: ReturnType<typeof setTimeout> | null = null;
let nativeStatusTimer: ReturnType<typeof setTimeout> | null = null;
let nativeEditor: OnlyOfficeEditorInstance | null = null;

const layoutStorageKey = computed(
  () => `docmind.template-split.${auth.user?.id ?? 'anonymous'}.${route.params.workspaceId}`,
);

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
const pageCount = computed(
  () => originalPreview.value?.preview.page_count ?? draft.value?.metadata.source_page_count ?? 1,
);

const nativeEditorHostId = computed(
  () => `docmind-native-editor-${templateId.value.replaceAll(/[^a-zA-Z0-9_-]/g, '-')}`,
);

const isRecord = (value: unknown): value is MutableNode =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const findNode = (value: unknown, id: string): MutableNode | null => {
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findNode(item, id);
      if (found !== null) return found;
    }
    return null;
  }
  if (!isRecord(value)) return null;
  if (value.id === id && typeof value.type === 'string') return value;
  for (const nested of Object.values(value)) {
    const found = findNode(nested, id);
    if (found !== null) return found;
  }
  return null;
};

const collectTextNodes = (node: MutableNode): MutableNode[] => {
  if (!Array.isArray(node.content)) return [];
  return node.content.filter(
    (item): item is MutableNode =>
      isRecord(item) && item.type === 'text' && typeof item.text === 'string',
  );
};

const collectEditableBlocks = (value: unknown, result: EditableBlock[] = []): EditableBlock[] => {
  if (Array.isArray(value)) {
    value.forEach((item) => collectEditableBlocks(item, result));
    return result;
  }
  if (!isRecord(value)) return result;
  if (
    (value.type === 'paragraph' || value.type === 'heading') &&
    typeof value.id === 'string' &&
    collectTextNodes(value).length > 0
  ) {
    const source = isRecord(value.source) ? value.source : null;
    result.push({
      id: value.id,
      type: value.type,
      text: collectTextNodes(value)
        .map((node) => String(node.text))
        .join(''),
      page: typeof source?.page_number === 'number' ? source.page_number : null,
    });
  }
  Object.values(value).forEach((nested) => collectEditableBlocks(nested, result));
  return result;
};

const editableBlocks = computed(() =>
  draft.value === null ? [] : collectEditableBlocks(draft.value),
);
const selectedBlock = computed(
  () => editableBlocks.value.find((block) => block.id === selectedNodeId.value) ?? null,
);

const selectedMutableNode = (): MutableNode | null => {
  if (draft.value === null || selectedNodeId.value === null) return null;
  return findNode(draft.value, selectedNodeId.value);
};

const ensureStyle = (node: MutableNode): MutableNode => {
  if (!isRecord(node.style)) node.style = {};
  return node.style as MutableNode;
};

const markChanged = (): void => {
  dirty.value = true;
  notice.value = '';
};

const updateBlockText = (value: string): void => {
  const node = selectedMutableNode();
  if (node === null) return;
  const textNodes = collectTextNodes(node);
  textNodes.forEach((textNode, index) => {
    textNode.text = index === 0 ? value : '';
  });
  markChanged();
};

const setAlignment = (event: Event): void => {
  const node = selectedMutableNode();
  if (node === null) return;
  ensureStyle(node).alignment = (event.target as HTMLSelectElement).value;
  markChanged();
};

const setFontSize = (event: Event): void => {
  const node = selectedMutableNode();
  if (node === null) return;
  const size = Math.max(6, Math.min(96, Number((event.target as HTMLInputElement).value)));
  collectTextNodes(node).forEach((textNode) => {
    ensureStyle(textNode).font_size = { value: size, unit: 'pt' };
  });
  markChanged();
};

const toggleBold = (): void => {
  const node = selectedMutableNode();
  if (node === null) return;
  const textNodes = collectTextNodes(node);
  const firstStyle = textNodes[0] && isRecord(textNodes[0].style) ? textNodes[0].style : {};
  const nextWeight = Number(firstStyle.font_weight ?? 400) >= 600 ? 400 : 700;
  textNodes.forEach((textNode) => {
    ensureStyle(textNode).font_weight = nextWeight;
  });
  markChanged();
};

const setMargin = (side: 'top' | 'right' | 'bottom' | 'left', event: Event): void => {
  if (draft.value === null) return;
  const value = Math.max(0, Math.min(100, Number((event.target as HTMLInputElement).value)));
  draft.value.page_layout.margins[side] = { value, unit: 'mm' };
  markChanged();
};

const paragraphAlignment = computed(() => {
  const node = selectedMutableNode();
  return node !== null && isRecord(node.style) && typeof node.style.alignment === 'string'
    ? node.style.alignment
    : 'left';
});
const paragraphFontSize = computed(() => {
  const node = selectedMutableNode();
  const text = node === null ? null : collectTextNodes(node)[0];
  const style = text !== null && isRecord(text?.style) ? text.style : null;
  const size = style !== null && isRecord(style.font_size) ? style.font_size.value : null;
  return typeof size === 'number' ? size : 11;
});

const injectResourceUrls = (html: string): string => {
  let result = html;
  const resources = selectedVersion.value?.resources ?? [];
  resources.forEach((resource, index) => {
    const url = resourceUrls.value[index];
    if (url === undefined) return;
    result = result.replaceAll(resource.download_url, url);
    result = result.replaceAll(
      `data-dm-resource-id="${resource.id}"`,
      `data-dm-resource-id="${resource.id}" src="${url}"`,
    );
  });
  return result;
};

const renderedDocument = computed(() => {
  if (draft.value === null || selectedVersion.value === null) return { html: '', css: '' };
  if (!dirty.value && serverHtml.value.length > 0) {
    return { html: serverHtml.value, css: selectedVersion.value.document.css };
  }
  try {
    const rendered = serializeControlledDocument(draft.value);
    return { html: injectResourceUrls(rendered.html), css: rendered.css };
  } catch {
    return { html: serverHtml.value, css: selectedVersion.value.document.css };
  }
});

const previewSrcdoc = computed(
  () => `<!doctype html><html><head>
  <meta charset="utf-8">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src blob: data:; style-src 'unsafe-inline'; font-src blob: data:">
  <style>${renderedDocument.value.css}html{background:#d8d7d2}body{margin:0;padding:28px;zoom:${zoom.value / 100}}.dm-document{box-shadow:0 8px 34px rgba(21,31,39,.16)}</style>
  </head><body>${renderedDocument.value.html}</body></html>`,
);

const originalSrc = computed(() =>
  originalObjectUrl.value === null
    ? null
    : `${originalObjectUrl.value}#page=${originalPage.value}&zoom=page-width`,
);

const diffChanges = computed<DiffChange[]>(() => {
  const changes = selectedVersion.value?.diff.changes;
  if (!Array.isArray(changes)) return [];
  const result: DiffChange[] = [];
  changes.forEach((change) => {
    if (
      isRecord(change) &&
      typeof change.path === 'string' &&
      ['added', 'removed', 'changed'].includes(String(change.kind))
    ) {
      result.push({
        path: change.path,
        kind: String(change.kind) as DiffChange['kind'],
      });
    }
  });
  return result;
});

const revokeResources = (): void => {
  resourceUrls.value.forEach((url) => URL.revokeObjectURL(url));
  resourceUrls.value = [];
};

const destroyNativeEditor = (): void => {
  if (nativeStatusTimer !== null) {
    clearTimeout(nativeStatusTimer);
    nativeStatusTimer = null;
  }
  nativeEditor?.destroyEditor();
  nativeEditor = null;
};

const loadOnlyOfficeScript = async (editorUrl: string): Promise<void> => {
  if (window.DocsAPI !== undefined) return;
  const source = `${editorUrl.replace(/\/$/, '')}/web-apps/apps/api/documents/api.js`;
  const existing = document.querySelector<HTMLScriptElement>('script[data-docmind-onlyoffice]');
  if (existing !== null) {
    await new Promise<void>((resolve, reject) => {
      if (window.DocsAPI !== undefined) {
        resolve();
        return;
      }
      existing.addEventListener('load', () => resolve(), { once: true });
      existing.addEventListener('error', () => reject(new Error('ONLYOFFICE SDK 加载失败')), {
        once: true,
      });
    });
    return;
  }
  await new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = source;
    script.async = true;
    script.dataset.docmindOnlyoffice = 'true';
    script.addEventListener('load', () => resolve(), { once: true });
    script.addEventListener(
      'error',
      () => {
        script.remove();
        reject(new Error('ONLYOFFICE SDK 加载失败，请确认原生编辑服务已启动'));
      },
      { once: true },
    );
    document.head.append(script);
  });
  if (window.DocsAPI === undefined) throw new Error('ONLYOFFICE SDK 未提供 DocsAPI');
};

const scheduleNativeStatusRefresh = (): void => {
  if (nativeStatusTimer !== null) clearTimeout(nativeStatusTimer);
  if (nativeSession.value === null) return;
  nativeStatusTimer = setTimeout(() => void refreshNativeStatus(true), 2500);
};

const refreshNativeStatus = async (continuePolling = false): Promise<void> => {
  if (nativeSession.value === null) return;
  try {
    nativeStatus.value = await getNativeEditorSessionStatus(nativeSession.value.session_id);
  } catch (caught) {
    const message = caught instanceof Error ? caught.message : '原生编辑保存状态读取失败';
    if (continuePolling) {
      destroyNativeEditor();
      nativeSession.value = null;
      nativeMode.value = false;
      error.value = `原生编辑会话已中断：${message}。请重新进入原生编辑。`;
    } else {
      error.value = message;
    }
  } finally {
    if (continuePolling && nativeMode.value) scheduleNativeStatusRefresh();
  }
};

const startNativeEditor = async (): Promise<void> => {
  if (!isCurrentVersion.value || nativeLoading.value) return;
  nativeLoading.value = true;
  error.value = '';
  notice.value = '';
  destroyNativeEditor();
  nativeSession.value = null;
  nativeStatus.value = null;
  nativeMode.value = true;
  editMode.value = false;
  try {
    await nextTick();
    const session = await createNativeEditorSession(templateId.value);
    nativeSession.value = session;
    await loadOnlyOfficeScript(session.editor_url);
    await nextTick();
    if (nativeEditorHost.value === null || window.DocsAPI === undefined) {
      throw new Error('原生编辑器容器初始化失败');
    }
    nativeEditor = new window.DocsAPI.DocEditor(nativeEditorHostId.value, session.editor_config);
    notice.value = '原生 DOCX 编辑 POC 已打开；编辑器内保存会触发受控回调并落入独立 POC 文件。';
    scheduleNativeStatusRefresh();
  } catch (caught) {
    destroyNativeEditor();
    nativeMode.value = false;
    error.value = caught instanceof Error ? caught.message : '原生编辑器打开失败';
  } finally {
    nativeLoading.value = false;
  }
};

const stopNativeEditor = (): void => {
  destroyNativeEditor();
  nativeMode.value = false;
  void refreshNativeStatus(false);
};

const prepareVersion = async (version: TemplateVersion): Promise<void> => {
  if (nativeMode.value) stopNativeEditor();
  selectedVersionId.value = version.id;
  try {
    const model: unknown = cloneJsonValue(version.document_model);
    assertValidControlledDocument(model);
    draft.value = model;
    selectedNodeId.value = collectEditableBlocks(model)[0]?.id ?? null;
    dirty.value = false;
    editMode.value = false;
    revokeResources();
    const urls = await Promise.all(
      version.resources.map((resource) => getAuthenticatedObjectUrl(resource.download_url)),
    );
    resourceUrls.value = urls;
    serverHtml.value = injectResourceUrls(version.document.html);
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '受控文档模型校验失败';
    draft.value = null;
  }
};

const scheduleRefresh = (): void => {
  if (refreshTimer !== null) clearTimeout(refreshTimer);
  const status = detail.value?.template.conversion_status;
  if (status !== undefined && ['queued', 'running', 'retrying'].includes(status)) {
    refreshTimer = setTimeout(load, 2500);
  }
};

const load = async (): Promise<void> => {
  error.value = '';
  try {
    const response = await getTemplate(templateId.value);
    const priorCurrent = detail.value?.current_version?.id ?? null;
    detail.value = response;
    if (response.current_version != null && priorCurrent !== response.current_version.id) {
      await prepareVersion(response.current_version);
    }
    scheduleRefresh();
    if (response.template.conversion_status === 'ready') await loadOriginalPreview();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '模板读取失败';
  } finally {
    loading.value = false;
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
    error.value = caught instanceof Error ? caught.message : '原件预览读取失败';
  }
};

const saveVersion = async (): Promise<void> => {
  if (draft.value === null || currentVersion.value === null || !isCurrentVersion.value) return;
  saving.value = true;
  error.value = '';
  try {
    assertValidControlledDocument(draft.value);
    const created = await createTemplateVersion(templateId.value, {
      base_version_id: currentVersion.value.id,
      document_model: cloneJsonValue(draft.value) as unknown as JsonObject,
      change_summary: changeSummary.value.trim(),
    });
    notice.value = `V${created.version_number} 已保存，后端 Diff 已固化。`;
    await load();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '模板版本保存失败';
  } finally {
    saving.value = false;
  }
};

const publish = async (): Promise<void> => {
  if (currentVersion.value === null || !isCurrentVersion.value || dirty.value) return;
  publishing.value = true;
  try {
    const published = await publishTemplateVersion(templateId.value, currentVersion.value.id, {
      note: '模板编辑器确认发布',
    });
    notice.value = `V${published.version_number} 已发布并可用于文档实例。`;
    await load();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '模板发布失败';
  } finally {
    publishing.value = false;
  }
};

const rollback = async (): Promise<void> => {
  if (selectedVersion.value === null || isCurrentVersion.value) return;
  rollingBack.value = true;
  try {
    const restored = await rollbackTemplate(templateId.value, {
      target_version_id: selectedVersion.value.id,
      change_summary: `恢复 V${selectedVersion.value.version_number}：${changeSummary.value.trim()}`,
    });
    notice.value = `已基于 V${selectedVersion.value.version_number} 创建并发布 V${restored.version_number}。`;
    await load();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '模板版本恢复失败';
  } finally {
    rollingBack.value = false;
  }
};

const focusNode = async (nodeId: string | null, page: number | null): Promise<void> => {
  if (nodeId !== null && editableBlocks.value.some((block) => block.id === nodeId)) {
    selectedNodeId.value = nodeId;
  }
  if (page !== null) originalPage.value = Math.max(1, Math.min(pageCount.value, page));
  await nextTick();
  if (nodeId === null) return;
  const selector = `[data-dm-node-id="${nodeId.replaceAll('"', '\\"')}"]`;
  rightFrame.value?.contentDocument?.querySelector(selector)?.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  });
};

const restoreLayout = (): void => {
  try {
    const stored = localStorage.getItem(layoutStorageKey.value);
    if (stored === null) return;
    const value = JSON.parse(stored) as { split?: unknown; left?: unknown; right?: unknown };
    if (typeof value.split === 'number') split.value = value.split;
    if (typeof value.left === 'boolean') leftCollapsed.value = value.left;
    if (typeof value.right === 'boolean') rightCollapsed.value = value.right;
  } catch {
    localStorage.removeItem(layoutStorageKey.value);
  }
};

watch([split, leftCollapsed, rightCollapsed], () => {
  localStorage.setItem(
    layoutStorageKey.value,
    JSON.stringify({ split: split.value, left: leftCollapsed.value, right: rightCollapsed.value }),
  );
});

onMounted(() => {
  restoreLayout();
  void load();
});
onUnmounted(() => {
  if (refreshTimer !== null) clearTimeout(refreshTimer);
  if (previewTimer !== null) clearTimeout(previewTimer);
  if (originalObjectUrl.value !== null) URL.revokeObjectURL(originalObjectUrl.value);
  destroyNativeEditor();
  revokeResources();
});
</script>

<template>
  <section class="template-editor-page">
    <header class="template-editor-header">
      <div>
        <button
          class="back-link"
          type="button"
          @click="
            router.push({ name: 'templates', params: { workspaceId: route.params.workspaceId } })
          "
        >
          ← 返回模板登记簿
        </button>
        <p class="eyebrow">
          TEMPLATE STUDIO / {{ detail?.template.conversion_status ?? 'LOADING' }}
        </p>
        <h1>{{ detail?.template.name ?? '模板编辑器' }}</h1>
      </div>
      <div v-if="currentVersion" class="template-header-actions">
        <label>变更说明<input v-model="changeSummary" maxlength="1000" /></label>
        <DmButton
          variant="secondary"
          :disabled="!dirty || !isCurrentVersion || changeSummary.trim().length === 0"
          :loading="saving"
          @click="saveVersion"
          >保存为新版本</DmButton
        >
        <DmButton
          :disabled="
            dirty ||
            !isCurrentVersion ||
            blockingWarnings.length > 0 ||
            currentVersion.status === 'published'
          "
          :loading="publishing"
          @click="publish"
          >确认并发布</DmButton
        >
      </div>
    </header>

    <InlineNotice v-if="error" tone="danger" title="模板工作台发生错误" :detail="error" />
    <InlineNotice v-if="notice" tone="success" title="操作已完成" :detail="notice" />

    <div v-if="loading" class="document-loading">正在载入模板转换状态…</div>
    <section
      v-else-if="detail && detail.template.conversion_status !== 'ready'"
      class="template-conversion-state"
    >
      <span class="conversion-engine" aria-hidden="true"><i></i><b>DOC</b></span>
      <p class="eyebrow">DETERMINISTIC CONVERSION</p>
      <h2 v-if="detail.template.conversion_status !== 'failed'">正在生成可编辑模板</h2>
      <h2 v-else>模板转换未完成</h2>
      <ol>
        <li class="done">不可变原件校验</li>
        <li :class="{ done: detail.template.conversion_status === 'retrying' }">
          生成 PDF 对照预览
        </li>
        <li>解析受控文档模型与资源</li>
        <li>白名单 HTML 与告警审查</li>
      </ol>
      <InlineNotice
        v-if="detail.template.failure_code"
        tone="danger"
        title="转换任务失败"
        :detail="`失败代码：${detail.template.failure_code}`"
      />
    </section>

    <template v-else-if="detail && selectedVersion">
      <section class="template-version-strip" aria-label="模板版本历史">
        <div><span>版本历史</span><small>选择旧版本可查看后端 Diff 或恢复</small></div>
        <button
          v-for="version in detail.versions"
          :key="version.id"
          type="button"
          :class="{ active: version.id === selectedVersionId }"
          @click="prepareVersion(version)"
        >
          <strong>V{{ version.version_number }}</strong>
          <span>{{ version.status }}</span>
          <small>{{ new Date(version.created_at).toLocaleDateString('zh-CN') }}</small>
        </button>
        <DmButton
          v-if="!isCurrentVersion"
          variant="secondary"
          size="small"
          :loading="rollingBack"
          @click="rollback"
          >恢复此版本</DmButton
        >
      </section>

      <section class="template-toolbar">
        <div class="mode-switch" aria-label="模板模式">
          <button
            type="button"
            :class="{ active: !editMode && !nativeMode }"
            @click="((editMode = false), stopNativeEditor())"
          >
            预览
          </button>
          <button
            type="button"
            :class="{ active: editMode && !nativeMode }"
            :disabled="!isCurrentVersion"
            @click="(stopNativeEditor(), (editMode = true))"
          >
            微调
          </button>
          <button
            type="button"
            :class="{ active: nativeMode }"
            :disabled="!isCurrentVersion || nativeLoading"
            @click="startNativeEditor"
          >
            {{ nativeLoading ? '正在启动…' : '原生编辑 POC' }}
          </button>
        </div>
        <label v-if="!nativeMode"
          >缩放 <input v-model.number="zoom" type="range" min="50" max="150" step="5" /><span
            >{{ zoom }}%</span
          ></label
        >
        <div class="page-navigator">
          <button type="button" :disabled="originalPage <= 1" @click="originalPage--">‹</button>
          <span>原件 P{{ originalPage }} / {{ pageCount }}</span>
          <button type="button" :disabled="originalPage >= pageCount" @click="originalPage++">
            ›
          </button>
        </div>
        <span v-if="nativeMode" class="template-save-state native">
          {{ nativeStatus?.status ?? '正在建立编辑会话' }}
          <template v-if="nativeStatus?.saved_at">· 已收到保存回调</template>
        </span>
        <span v-else class="template-save-state" :class="{ dirty }">{{
          dirty ? '有未保存修改' : '版本已固化'
        }}</span>
      </section>

      <DmSplitPane
        v-model="split"
        v-model:left-collapsed="leftCollapsed"
        v-model:right-collapsed="rightCollapsed"
        left-label="不可变原件"
        right-label="受控模板"
        class="template-split"
      >
        <template #left>
          <section class="template-original-pane">
            <header>
              <span
                ><small>IMMUTABLE ORIGINAL</small><strong>原件 · P{{ originalPage }}</strong></span
              ><DmStatus tone="info" label="只读" />
            </header>
            <iframe v-if="originalSrc" :src="originalSrc" title="不可变 PDF 原件"></iframe>
            <div v-else class="preview-placeholder">
              <span class="paper-stack" aria-hidden="true"><i></i><i></i><i></i></span
              ><strong>正在载入安全 PDF 预览</strong>
            </div>
          </section>
        </template>
        <template #right>
          <section
            class="template-controlled-pane"
            :class="{ 'is-editing': editMode && !nativeMode, 'is-native': nativeMode }"
          >
            <section v-if="nativeMode" class="native-editor-shell">
              <header class="native-editor-session-bar">
                <span>
                  <small>NATIVE DOCX / ISOLATED POC</small>
                  <strong>分页与字符级富文本编辑</strong>
                </span>
                <span class="native-editor-session-meta">
                  <code>{{ nativeStatus?.status ?? 'loading' }}</code>
                  <button type="button" @click="refreshNativeStatus(false)">检查保存</button>
                  <button type="button" @click="stopNativeEditor">退出 POC</button>
                </span>
              </header>
              <div class="native-editor-poc-note">
                当前保存写入独立 POC 对象，不改变已发布模板版本；G0 通过后再接正式版本状态机。
              </div>
              <div
                :id="nativeEditorHostId"
                ref="nativeEditorHost"
                class="native-editor-host"
                aria-label="ONLYOFFICE 原生 DOCX 编辑器"
              ></div>
            </section>
            <aside v-else-if="editMode" class="template-inspector">
              <header><small>CONTROLLED NODES</small><strong>结构与微调</strong></header>
              <label class="inspector-field"
                >编辑段落
                <select v-model="selectedNodeId">
                  <option
                    v-for="(block, index) in editableBlocks"
                    :key="block.id"
                    :value="block.id"
                  >
                    {{ index + 1 }} · {{ block.text.slice(0, 24) || '空段落' }}
                  </option>
                </select>
              </label>
              <template v-if="selectedBlock">
                <label class="inspector-field"
                  >文字内容<textarea
                    :value="selectedBlock.text"
                    rows="7"
                    @input="updateBlockText(($event.target as HTMLTextAreaElement).value)"
                  ></textarea>
                </label>
                <div class="inspector-grid">
                  <label
                    >字号（pt）<input
                      type="number"
                      min="6"
                      max="96"
                      :value="paragraphFontSize"
                      @change="setFontSize"
                  /></label>
                  <label
                    >对齐<select :value="paragraphAlignment" @change="setAlignment">
                      <option value="left">左对齐</option>
                      <option value="center">居中</option>
                      <option value="right">右对齐</option>
                      <option value="justify">两端对齐</option>
                    </select></label
                  >
                </div>
                <button class="format-toggle" type="button" @click="toggleBold">切换粗体</button>
              </template>
              <fieldset>
                <legend>页边距（mm）</legend>
                <label v-for="side in ['top', 'right', 'bottom', 'left'] as const" :key="side"
                  >{{ { top: '上', right: '右', bottom: '下', left: '左' }[side]
                  }}<input
                    type="number"
                    min="0"
                    max="100"
                    :value="draft?.page_layout.margins[side].value"
                    @change="setMargin(side, $event)"
                /></label>
              </fieldset>
            </aside>
            <div v-if="!nativeMode" class="template-canvas">
              <iframe
                ref="rightFrame"
                sandbox="allow-same-origin"
                :srcdoc="previewSrcdoc"
                title="可编辑模板预览"
              ></iframe>
            </div>
            <aside v-if="!nativeMode" class="template-audit-drawer">
              <details open>
                <summary>
                  转换告警 <strong>{{ selectedVersion.warnings.length }}</strong>
                </summary>
                <button
                  v-for="warning in selectedVersion.warnings"
                  :key="warning.id"
                  type="button"
                  :class="`warning-${warning.severity}`"
                  @click="focusNode(warning.source_node_id, warning.page_number)"
                >
                  <span>{{ warning.code }}</span
                  ><strong>{{ warning.message }}</strong
                  ><small
                    >{{ warning.page_number ? `P${warning.page_number}` : '全局' }} ·
                    {{ warning.blocking ? '阻断发布' : '可接受回退' }}</small
                  >
                </button>
                <p v-if="selectedVersion.warnings.length === 0">未发现版式转换告警。</p>
              </details>
              <details>
                <summary>
                  后端版本 Diff <strong>{{ diffChanges.length }}</strong>
                </summary>
                <ol>
                  <li
                    v-for="change in diffChanges.slice(0, 100)"
                    :key="`${change.kind}-${change.path}`"
                  >
                    <span>{{ change.kind }}</span
                    ><code>{{ change.path }}</code>
                  </li>
                </ol>
                <p v-if="diffChanges.length === 0">首版或本版本没有结构差异。</p>
              </details>
            </aside>
          </section>
        </template>
      </DmSplitPane>
    </template>
  </section>
</template>
