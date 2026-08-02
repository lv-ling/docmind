import type { JsonObject, TemplateId, TemplateVersion } from '@/contracts';
import { assertValidControlledDocument, type ControlledDocument } from '@/editor';
import { ref, type ComputedRef, type Ref } from 'vue';

import {
  createTemplateVersion,
  publishTemplateVersion,
  rollbackTemplate,
} from '@/api/templates.js';
import { cloneJsonValue } from '@/utils/json.js';

interface TemplateVersionActionsOptions {
  templateId: ComputedRef<TemplateId>;
  draft: Ref<ControlledDocument | null>;
  currentVersion: ComputedRef<TemplateVersion | null>;
  selectedVersion: ComputedRef<TemplateVersion | null>;
  isCurrentVersion: ComputedRef<boolean>;
  hasUnsavedChanges: Ref<boolean>;
  changeSummary: Ref<string>;
  editorError: Ref<string>;
  editorNotice: Ref<string>;
  reloadTemplate: () => Promise<void>;
}

export const useTemplateVersionActions = ({
  templateId,
  draft,
  currentVersion,
  selectedVersion,
  isCurrentVersion,
  hasUnsavedChanges,
  changeSummary,
  editorError,
  editorNotice,
  reloadTemplate,
}: TemplateVersionActionsOptions) => {
  const isSavingVersion = ref(false);
  const isPublishingVersion = ref(false);
  const isRollingBackVersion = ref(false);

  const saveVersion = async (): Promise<void> => {
    if (draft.value === null || currentVersion.value === null || !isCurrentVersion.value) return;
    isSavingVersion.value = true;
    editorError.value = '';
    try {
      assertValidControlledDocument(draft.value);
      const created = await createTemplateVersion(templateId.value, {
        base_version_id: currentVersion.value.id,
        document_model: cloneJsonValue(draft.value) as unknown as JsonObject,
        change_summary: changeSummary.value.trim(),
      });
      editorNotice.value = `V${created.version_number} 已保存，后端 Diff 已固化。`;
      await reloadTemplate();
    } catch (caught) {
      editorError.value = caught instanceof Error ? caught.message : '模板版本保存失败';
    } finally {
      isSavingVersion.value = false;
    }
  };

  const publishVersion = async (): Promise<void> => {
    if (currentVersion.value === null || !isCurrentVersion.value || hasUnsavedChanges.value) {
      return;
    }
    isPublishingVersion.value = true;
    try {
      const published = await publishTemplateVersion(templateId.value, currentVersion.value.id, {
        note: '模板编辑器确认发布',
      });
      editorNotice.value = `V${published.version_number} 已发布并可用于文档实例。`;
      await reloadTemplate();
    } catch (caught) {
      editorError.value = caught instanceof Error ? caught.message : '模板发布失败';
    } finally {
      isPublishingVersion.value = false;
    }
  };

  const rollbackVersion = async (): Promise<void> => {
    if (selectedVersion.value === null || isCurrentVersion.value) return;
    isRollingBackVersion.value = true;
    try {
      const restored = await rollbackTemplate(templateId.value, {
        target_version_id: selectedVersion.value.id,
        change_summary: `恢复 V${selectedVersion.value.version_number}：${changeSummary.value.trim()}`,
      });
      editorNotice.value = `已基于 V${selectedVersion.value.version_number} 创建并发布 V${restored.version_number}。`;
      await reloadTemplate();
    } catch (caught) {
      editorError.value = caught instanceof Error ? caught.message : '模板版本恢复失败';
    } finally {
      isRollingBackVersion.value = false;
    }
  };

  return {
    isSavingVersion,
    isPublishingVersion,
    isRollingBackVersion,
    saveVersion,
    publishVersion,
    rollbackVersion,
  };
};
