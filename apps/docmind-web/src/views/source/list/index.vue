<script setup lang="ts">
import { DmButton } from '@/ui';

import AppIcon from '@/components/AppIcon.vue';

import SourceRegister from './components/SourceRegister.vue';
import SourceTaskPanel from './components/SourceTaskPanel.vue';
import SourceUploadDialog from './components/SourceUploadDialog.vue';
import { useSourceRegistry } from './composables/useSourceRegistry.js';
import { useSourceUpload } from './composables/useSourceUpload.js';

const {
  sources,
  isLoadingSources,
  sourceLoadError,
  searchQuery,
  sourceFilter,
  currentPage,
  filteredSources,
  totalPages,
  safeCurrentPage,
  pagedSources,
  pageStart,
  pageEnd,
  pageNumbers,
  selectedSource,
  selectSource,
  openSelectedSource,
  startSelectedExtraction,
  resetFilters,
  loadSources,
} = useSourceRegistry();

const {
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
} = useSourceUpload(loadSources);
</script>

<template>
  <section class="page-stack source-page">
    <header class="page-heading source-page-heading">
      <div>
        <h1>文档中心</h1>
        <p>集中管理不可变原件、版本和处理入口。</p>
      </div>
      <DmButton type="button" size="small" @click="openUploadDialog">
        <AppIcon name="plus" /><span>上传文档</span>
      </DmButton>
    </header>

    <div class="source-workbench">
      <SourceRegister
        v-model:search-query="searchQuery"
        v-model:source-filter="sourceFilter"
        v-model:current-page="currentPage"
        :sources="sources"
        :filtered-sources="filteredSources"
        :paged-sources="pagedSources"
        :selected-source-id="selectedSource?.id ?? null"
        :is-loading="isLoadingSources"
        :load-error="sourceLoadError"
        :total-pages="totalPages"
        :safe-current-page="safeCurrentPage"
        :page-start="pageStart"
        :page-end="pageEnd"
        :page-numbers="pageNumbers"
        @select="selectSource"
        @reset="resetFilters"
        @reload="loadSources"
      />
      <SourceTaskPanel
        :source="selectedSource"
        @open-source="openSelectedSource"
        @start-extraction="startSelectedExtraction"
      />
    </div>

    <SourceUploadDialog
      v-model:document-name="documentName"
      :is-open="isUploadDialogOpen"
      :selected-file="selectedFile"
      :upload-stage="uploadStage"
      :upload-progress="uploadProgress"
      :upload-error="uploadError"
      :is-uploading="isUploading"
      :upload-stage-label="uploadStageLabel"
      @close="closeUploadDialog"
      @select-file="selectUploadFile"
      @submit="uploadSource"
    />
  </section>
</template>

<style src="./styles.css"></style>
