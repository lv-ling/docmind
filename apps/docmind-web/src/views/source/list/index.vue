<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import { RouteName } from '@/router/constants.js';
import { DmButton, DmInput, DmInteractiveSurface, DmSelect, DmStatus, DmTabs } from '@/ui';

import SourceUploadDialog from './components/SourceUploadDialog.vue';
import { useSourceUpload } from './composables/useSourceUpload.js';
import {
  DOCUMENT_CATEGORY_TABS,
  DOCUMENT_CENTER_ITEMS,
  filterDocumentCenterItems,
  type DocumentCategory,
} from './model/source-list.js';

const route = useRoute();
const router = useRouter();
const activeCategory = ref<DocumentCategory>('all');
const searchQuery = ref(typeof route.query.q === 'string' ? route.query.q : '');
const fileType = ref('all');
const isFilterOpen = ref(false);

const filteredDocuments = computed(() =>
  filterDocumentCenterItems(
    DOCUMENT_CENTER_ITEMS,
    searchQuery.value,
    activeCategory.value,
    fileType.value,
  ),
);

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
} = useSourceUpload(async () => undefined);

const openDocument = async (sourceId: string): Promise<void> => {
  await router.push({ name: RouteName.SourceDetail, query: { sourceId } });
};

watch(
  () => route.query.q,
  (value) => {
    searchQuery.value = typeof value === 'string' ? value : '';
  },
);
</script>

<template>
  <section class="flex min-h-full animate-dm-page-fade flex-col bg-white text-zinc-900">
    <header
      class="sticky top-0 z-10 flex h-16 box-border items-center justify-between border-b border-zinc-200 bg-white/90 px-6 py-4 backdrop-blur-sm"
    >
      <div>
        <h1 class="text-[15px] leading-5 font-semibold">文档中心</h1>
        <p class="mt-0.5 text-[11px] text-zinc-500">统一管理原始文档与 AI 结构化处理状态</p>
      </div>
      <div class="flex items-center gap-2">
        <DmButton variant="secondary" @click="isFilterOpen = !isFilterOpen">
          <AppIcon name="filter" />筛选
        </DmButton>
        <DmButton @click="openUploadDialog"><AppIcon name="upload" />上传文档</DmButton>
      </div>
    </header>

    <div class="mx-auto w-full max-w-[1600px] p-6">
      <div class="mb-5 flex items-end justify-between gap-6">
        <DmTabs
          v-model="activeCategory"
          class="min-w-0 flex-1"
          :items="DOCUMENT_CATEGORY_TABS"
          label="文档分类"
        />
        <label class="relative mb-2 block w-72 shrink-0">
          <AppIcon
            name="search"
            class="absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-zinc-400"
          />
          <span class="dm-sr-only">搜索文档</span>
          <DmInput
            v-model="searchQuery"
            appearance="unstyled"
            type="search"
            placeholder="搜索文档、分类或模板..."
            class="h-[30px] w-full rounded-compact border border-zinc-200 bg-white pr-3 pl-8 text-[12px] text-zinc-900 shadow-subtle outline-none placeholder:text-zinc-400 focus:border-brand-400 focus:ring-2 focus:ring-brand-100/50"
          />
        </label>
      </div>

      <div
        v-if="isFilterOpen"
        class="mb-4 flex items-center justify-between rounded-lg border border-zinc-200 bg-zinc-50 px-4 py-3 text-[12px]"
      >
        <div class="flex items-center gap-3">
          <span class="font-medium text-zinc-700">文件类型</span>
          <DmSelect id="document-file-type" v-model="fileType" aria-label="文件类型">
            <option value="all">全部类型</option>
            <option value="PDF">PDF</option>
            <option value="DOCX">DOCX</option>
            <option value="XLSX">XLSX</option>
          </DmSelect>
        </div>
        <DmButton variant="ghost" @click="fileType = 'all'">重置筛选</DmButton>
      </div>

      <div class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
        <div
          class="grid grid-cols-[minmax(0,5fr)_minmax(150px,2fr)_minmax(120px,2fr)_90px_140px] items-center border-b border-zinc-200 bg-zinc-50 px-4 py-2.5 text-[11px] font-semibold tracking-wider text-zinc-500 uppercase"
        >
          <span>文档名称</span><span>分类 / 模板</span><span>AI 状态</span><span>置信度</span
          ><span class="text-right">更新时间</span>
        </div>

        <DmInteractiveSurface
          v-for="document in filteredDocuments"
          :key="document.id"
          type="button"
          class="group grid w-full grid-cols-[minmax(0,5fr)_minmax(150px,2fr)_minmax(120px,2fr)_90px_140px] items-center border-b border-zinc-100 px-4 py-3 text-left transition-colors last:border-b-0 hover:bg-zinc-50 focus-visible:outline-2 focus-visible:outline-brand-500 focus-visible:outline-offset-[-2px]"
          @click="openDocument(document.id)"
        >
          <span class="flex min-w-0 items-center gap-3 pr-4">
            <span
              class="flex size-8 shrink-0 items-center justify-center rounded-md border border-zinc-200 bg-zinc-50 text-zinc-400 transition-colors group-hover:border-brand-200 group-hover:bg-brand-50 group-hover:text-brand-600"
            >
              <AppIcon name="file-text" class="size-4" />
            </span>
            <span class="min-w-0">
              <strong class="block truncate text-[13px] font-medium text-zinc-900">{{
                document.name
              }}</strong>
              <small class="mt-0.5 block text-[10px] text-zinc-400"
                >{{ document.fileType }} · {{ document.pages }} 页</small
              >
            </span>
          </span>
          <span class="min-w-0 pr-3">
            <strong class="block truncate text-[12px] font-normal text-zinc-900">{{
              document.category
            }}</strong>
            <small class="mt-0.5 block truncate text-[11px] text-zinc-500">{{
              document.template
            }}</small>
          </span>
          <DmStatus :tone="document.statusTone" :label="document.aiStatus" />
          <span class="font-mono text-[12px] text-zinc-900">{{ document.confidence }}%</span>
          <span class="flex items-center justify-end gap-2 text-[11px] text-zinc-500">
            {{ document.updatedAt }}
            <AppIcon
              name="chevron-right"
              class="size-3.5 text-zinc-300 opacity-0 transition-opacity group-hover:opacity-100"
            />
          </span>
        </DmInteractiveSurface>

        <div v-if="filteredDocuments.length === 0" class="px-6 py-16 text-center">
          <AppIcon name="file-search" class="mx-auto size-5 text-zinc-300" />
          <p class="mt-3 text-[12px] font-medium text-zinc-700">没有匹配的文档</p>
          <p class="mt-1 text-[11px] text-zinc-400">调整搜索关键词或筛选条件后重试</p>
        </div>
      </div>
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
