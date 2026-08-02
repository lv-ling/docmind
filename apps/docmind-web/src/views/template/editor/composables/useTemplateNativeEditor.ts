import type { TemplateId } from '@/contracts';
import { computed, nextTick, onUnmounted, ref, type ComputedRef, type Ref } from 'vue';

import {
  createNativeEditorSession,
  getNativeEditorSessionStatus,
  type NativeEditorSession,
  type NativeEditorSessionStatus,
} from '@/api/templates.js';

interface TemplateNativeEditorOptions {
  templateId: ComputedRef<TemplateId>;
  isCurrentVersion: ComputedRef<boolean>;
  isEditMode: Ref<boolean>;
  editorError: Ref<string>;
  editorNotice: Ref<string>;
}

export const useTemplateNativeEditor = ({
  templateId,
  isCurrentVersion,
  isEditMode,
  editorError,
  editorNotice,
}: TemplateNativeEditorOptions) => {
  const isNativeMode = ref(false);
  const isLoadingNativeEditor = ref(false);
  const nativeSession = ref<NativeEditorSession | null>(null);
  const nativeStatus = ref<NativeEditorSessionStatus | null>(null);
  const nativeEditorHostRef = ref<HTMLDivElement | null>(null);
  let nativeStatusTimer: ReturnType<typeof setTimeout> | null = null;
  let nativeEditor: OnlyOfficeEditorInstance | null = null;

  const nativeEditorHostId = computed(
    () => `docmind-native-editor-${templateId.value.replaceAll(/[^a-zA-Z0-9_-]/g, '-')}`,
  );

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

  const refreshNativeStatus = async (shouldContinuePolling = false): Promise<void> => {
    if (nativeSession.value === null) return;
    try {
      nativeStatus.value = await getNativeEditorSessionStatus(nativeSession.value.session_id);
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : '原生编辑保存状态读取失败';
      if (shouldContinuePolling) {
        destroyNativeEditor();
        nativeSession.value = null;
        isNativeMode.value = false;
        editorError.value = `原生编辑会话已中断：${message}。请重新进入原生编辑。`;
      } else {
        editorError.value = message;
      }
    } finally {
      if (shouldContinuePolling && isNativeMode.value) scheduleNativeStatusRefresh();
    }
  };

  const startNativeEditor = async (): Promise<void> => {
    if (!isCurrentVersion.value || isLoadingNativeEditor.value) return;
    isLoadingNativeEditor.value = true;
    editorError.value = '';
    editorNotice.value = '';
    destroyNativeEditor();
    nativeSession.value = null;
    nativeStatus.value = null;
    isNativeMode.value = true;
    isEditMode.value = false;
    try {
      await nextTick();
      const session = await createNativeEditorSession(templateId.value);
      nativeSession.value = session;
      await loadOnlyOfficeScript(session.editor_url);
      await nextTick();
      if (nativeEditorHostRef.value === null || window.DocsAPI === undefined) {
        throw new Error('原生编辑器容器初始化失败');
      }
      nativeEditor = new window.DocsAPI.DocEditor(nativeEditorHostId.value, session.editor_config);
      editorNotice.value =
        '原生 DOCX 编辑 POC 已打开；编辑器内保存会触发受控回调并落入独立 POC 文件。';
      scheduleNativeStatusRefresh();
    } catch (caught) {
      destroyNativeEditor();
      isNativeMode.value = false;
      editorError.value = caught instanceof Error ? caught.message : '原生编辑器打开失败';
    } finally {
      isLoadingNativeEditor.value = false;
    }
  };

  const stopNativeEditor = (): void => {
    destroyNativeEditor();
    isNativeMode.value = false;
    void refreshNativeStatus(false);
  };

  onUnmounted(destroyNativeEditor);

  return {
    isNativeMode,
    isLoadingNativeEditor,
    nativeSession,
    nativeStatus,
    nativeEditorHostRef,
    nativeEditorHostId,
    refreshNativeStatus,
    startNativeEditor,
    stopNativeEditor,
  };
};
