import type { TemplateVersion } from '@/contracts';
import { serializeControlledDocument, type ControlledDocument } from '@/editor';
import { computed, ref, type ComputedRef, type Ref } from 'vue';

import {
  collectDocumentTextNodes,
  collectEditableDocumentBlocks,
  createTemplatePreviewSrcdoc,
  ensureDocumentNodeStyle,
  findDocumentNode,
  injectTemplateResourceUrls,
  isDocumentRecord,
  parseTemplateDiffChanges,
} from '../model/template-document.js';

interface TemplateDocumentEditorOptions {
  draft: Ref<ControlledDocument | null>;
  selectedVersion: ComputedRef<TemplateVersion | null>;
  serverHtml: Ref<string>;
  resourceUrls: Ref<string[]>;
  zoomPercentage: Ref<number>;
  editorNotice: Ref<string>;
}

export const useTemplateDocumentEditor = ({
  draft,
  selectedVersion,
  serverHtml,
  resourceUrls,
  zoomPercentage,
  editorNotice,
}: TemplateDocumentEditorOptions) => {
  const selectedNodeId = ref<string | null>(null);
  const hasUnsavedChanges = ref(false);
  const isEditMode = ref(false);

  const editableBlocks = computed(() =>
    draft.value === null ? [] : collectEditableDocumentBlocks(draft.value),
  );
  const selectedBlock = computed(
    () => editableBlocks.value.find((block) => block.id === selectedNodeId.value) ?? null,
  );

  const getSelectedMutableNode = () => {
    if (draft.value === null || selectedNodeId.value === null) return null;
    return findDocumentNode(draft.value, selectedNodeId.value);
  };

  const markDocumentChanged = (): void => {
    hasUnsavedChanges.value = true;
    editorNotice.value = '';
  };

  const updateBlockText = (value: string): void => {
    const node = getSelectedMutableNode();
    if (node === null) return;
    const textNodes = collectDocumentTextNodes(node);
    textNodes.forEach((textNode, index) => {
      textNode.text = index === 0 ? value : '';
    });
    markDocumentChanged();
  };

  const setParagraphAlignment = (alignment: string): void => {
    const node = getSelectedMutableNode();
    if (node === null) return;
    ensureDocumentNodeStyle(node).alignment = alignment;
    markDocumentChanged();
  };

  const setParagraphFontSize = (requestedSize: number): void => {
    const node = getSelectedMutableNode();
    if (node === null) return;
    const size = Math.max(6, Math.min(96, requestedSize));
    collectDocumentTextNodes(node).forEach((textNode) => {
      ensureDocumentNodeStyle(textNode).font_size = { value: size, unit: 'pt' };
    });
    markDocumentChanged();
  };

  const toggleParagraphBold = (): void => {
    const node = getSelectedMutableNode();
    if (node === null) return;
    const textNodes = collectDocumentTextNodes(node);
    const firstStyle =
      textNodes[0] && isDocumentRecord(textNodes[0].style) ? textNodes[0].style : {};
    const nextWeight = Number(firstStyle.font_weight ?? 400) >= 600 ? 400 : 700;
    textNodes.forEach((textNode) => {
      ensureDocumentNodeStyle(textNode).font_weight = nextWeight;
    });
    markDocumentChanged();
  };

  const setPageMargin = (
    side: 'top' | 'right' | 'bottom' | 'left',
    requestedValue: number,
  ): void => {
    if (draft.value === null) return;
    const value = Math.max(0, Math.min(100, requestedValue));
    draft.value.page_layout.margins[side] = { value, unit: 'mm' };
    markDocumentChanged();
  };

  const paragraphAlignment = computed(() => {
    const node = getSelectedMutableNode();
    return node !== null && isDocumentRecord(node.style) && typeof node.style.alignment === 'string'
      ? node.style.alignment
      : 'left';
  });
  const paragraphFontSize = computed(() => {
    const node = getSelectedMutableNode();
    const text = node === null ? null : collectDocumentTextNodes(node)[0];
    const style = text !== null && isDocumentRecord(text?.style) ? text.style : null;
    const size = style !== null && isDocumentRecord(style.font_size) ? style.font_size.value : null;
    return typeof size === 'number' ? size : 11;
  });

  const renderedDocument = computed(() => {
    if (draft.value === null || selectedVersion.value === null) return { html: '', css: '' };
    if (!hasUnsavedChanges.value && serverHtml.value.length > 0) {
      return { html: serverHtml.value, css: selectedVersion.value.document.css };
    }
    try {
      const rendered = serializeControlledDocument(draft.value);
      return {
        html: injectTemplateResourceUrls(
          rendered.html,
          selectedVersion.value.resources,
          resourceUrls.value,
        ),
        css: rendered.css,
      };
    } catch {
      return { html: serverHtml.value, css: selectedVersion.value.document.css };
    }
  });

  const previewSrcdoc = computed(() =>
    createTemplatePreviewSrcdoc(
      renderedDocument.value.html,
      renderedDocument.value.css,
      zoomPercentage.value,
    ),
  );
  const diffChanges = computed(() => parseTemplateDiffChanges(selectedVersion.value?.diff.changes));

  return {
    selectedNodeId,
    hasUnsavedChanges,
    isEditMode,
    editableBlocks,
    selectedBlock,
    paragraphAlignment,
    paragraphFontSize,
    previewSrcdoc,
    diffChanges,
    updateBlockText,
    setParagraphAlignment,
    setParagraphFontSize,
    toggleParagraphBold,
    setPageMargin,
  };
};
