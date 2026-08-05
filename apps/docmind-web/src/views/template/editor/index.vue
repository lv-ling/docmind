<script setup lang="ts">
import { DmSplitPane, DmStatus } from '@/ui';

import { InlineNotice } from '@/components/index.js';

import {
  TemplateAuditDrawer,
  TemplateConversionState,
  TemplateEditorHeader,
  TemplateInspector,
  TemplateToolbar,
  TemplateVersionStrip,
} from './components/index.js';
import { useTemplateEditor } from './composables/useTemplateEditor.js';

const {
  detail,
  selectedVersionId,
  draft,
  isLoadingTemplate,
  isSavingVersion,
  isPublishingVersion,
  isRollingBackVersion,
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
  isNativeMode,
  isLoadingNativeEditor,
  nativeStatus,
  nativeEditorHostRef,
  nativeEditorHostId,
  refreshNativeStatus,
  startNativeEditor,
  stopNativeEditor,
  prepareVersion,
  saveVersion,
  publishVersion,
  rollbackVersion,
  focusNode,
  showPreviewMode,
  showEditMode,
  openTemplateList,
} = useTemplateEditor();
</script>

<template>
  <section class="template-editor-page">
    <TemplateEditorHeader
      v-model:change-summary="changeSummary"
      :template-name="detail?.template.name ?? '模板编辑器'"
      :conversion-status="detail?.template.conversion_status ?? 'LOADING'"
      :current-version="currentVersion"
      :has-unsaved-changes="hasUnsavedChanges"
      :is-current-version="isCurrentVersion"
      :blocking-warning-count="blockingWarnings.length"
      :is-saving-version="isSavingVersion"
      :is-publishing-version="isPublishingVersion"
      @back="openTemplateList"
      @save="saveVersion"
      @publish="publishVersion"
    />

    <InlineNotice
      v-if="editorError"
      tone="danger"
      title="模板工作台发生错误"
      :detail="editorError"
    />
    <InlineNotice v-if="editorNotice" tone="success" title="操作已完成" :detail="editorNotice" />

    <div v-if="isLoadingTemplate" class="document-loading">正在载入模板转换状态…</div>
    <TemplateConversionState
      v-else-if="detail && detail.template.conversion_status !== 'ready'"
      :status="detail.template.conversion_status"
      :failure-code="detail.template.failure_code"
    />

    <template v-else-if="detail && selectedVersion">
      <TemplateVersionStrip
        :versions="detail.versions"
        :selected-version-id="selectedVersionId"
        :is-current-version="isCurrentVersion"
        :is-rolling-back="isRollingBackVersion"
        @select-version="prepareVersion"
        @rollback="rollbackVersion"
      />

      <TemplateToolbar
        v-model:zoom-percentage="zoomPercentage"
        v-model:original-page="originalPage"
        :is-edit-mode="isEditMode"
        :is-native-mode="isNativeMode"
        :is-current-version="isCurrentVersion"
        :is-loading-native-editor="isLoadingNativeEditor"
        :native-status="nativeStatus"
        :has-unsaved-changes="hasUnsavedChanges"
        :page-count="pageCount"
        @show-preview="showPreviewMode"
        @show-edit="showEditMode"
        @start-native-editor="startNativeEditor"
      />

      <DmSplitPane
        v-model="splitPercentage"
        v-model:left-collapsed="isLeftPanelCollapsed"
        v-model:right-collapsed="isRightPanelCollapsed"
        left-label="不可变原件"
        right-label="受控模板"
        class="template-split"
      >
        <template #left>
          <section class="template-original-pane">
            <header>
              <span
                ><small>IMMUTABLE ORIGINAL</small><strong>原件 · P{{ originalPage }}</strong></span
              >
              <DmStatus tone="info" label="只读" />
            </header>
            <iframe v-if="originalSrc" :src="originalSrc" title="不可变 PDF 原件"></iframe>
            <div v-else class="preview-placeholder">
              <span class="paper-stack" aria-hidden="true"><i></i><i></i><i></i></span>
              <strong>正在载入安全 PDF 预览</strong>
            </div>
          </section>
        </template>

        <template #right>
          <section
            class="template-controlled-pane"
            :class="{ 'is-editing': isEditMode && !isNativeMode, 'is-native': isNativeMode }"
          >
            <section v-if="isNativeMode" class="native-editor-shell">
              <header class="native-editor-session-bar">
                <span
                  ><small>NATIVE DOCX / ISOLATED POC</small
                  ><strong>分页与字符级富文本编辑</strong></span
                >
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
                ref="nativeEditorHostRef"
                class="native-editor-host"
                aria-label="ONLYOFFICE 原生 DOCX 编辑器"
              ></div>
            </section>

            <TemplateInspector
              v-else-if="isEditMode"
              v-model:selected-node-id="selectedNodeId"
              :draft="draft"
              :editable-blocks="editableBlocks"
              :selected-block="selectedBlock"
              :paragraph-font-size="paragraphFontSize"
              :paragraph-alignment="paragraphAlignment"
              @update-text="updateBlockText"
              @update-font-size="setParagraphFontSize"
              @update-alignment="setParagraphAlignment"
              @toggle-bold="toggleParagraphBold"
              @update-margin="setPageMargin"
            />

            <div v-if="!isNativeMode" class="template-canvas">
              <iframe
                ref="previewFrameRef"
                sandbox="allow-same-origin"
                :srcdoc="previewSrcdoc"
                title="可编辑模板预览"
              ></iframe>
            </div>
            <TemplateAuditDrawer
              v-if="!isNativeMode"
              :version="selectedVersion"
              :diff-changes="diffChanges"
              @focus-node="focusNode"
            />
          </section>
        </template>
      </DmSplitPane>
    </template>
  </section>
</template>

<style src="./styles.css"></style>
