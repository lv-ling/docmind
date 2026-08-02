<script setup lang="ts">
import type { SourceDocument } from '@/contracts';
import { DmButton, DmInput, DmInteractiveSurface, DmStatus } from '@/ui';

import AppIcon from '@/components/AppIcon.vue';
import InlineNotice from '@/components/InlineNotice.vue';

import {
  formatSourceDate,
  getSourceVersionLabel,
  type SourceFilter,
} from '../model/source-list.js';

const searchQuery = defineModel<string>('searchQuery', { required: true });
const sourceFilter = defineModel<SourceFilter>('sourceFilter', { required: true });
const currentPage = defineModel<number>('currentPage', { required: true });

defineProps<{
  sources: SourceDocument[];
  filteredSources: SourceDocument[];
  pagedSources: SourceDocument[];
  selectedSourceId: string | null;
  isLoading: boolean;
  loadError: string;
  totalPages: number;
  safeCurrentPage: number;
  pageStart: number;
  pageEnd: number;
  pageNumbers: number[];
}>();

const emit = defineEmits<{
  select: [source: SourceDocument];
  reset: [];
  reload: [];
}>();
</script>

<template>
  <section class="source-register" aria-label="文档登记簿">
    <div class="source-list-controls">
      <div class="source-tabs" role="group" aria-label="文档状态筛选">
        <DmButton
          variant="ghost"
          :class="{ active: sourceFilter === 'all' }"
          @click="sourceFilter = 'all'"
        >
          全部文档 <span>{{ sources.length }}</span>
        </DmButton>
        <DmButton
          variant="ghost"
          :class="{ active: sourceFilter === 'registered' }"
          @click="sourceFilter = 'registered'"
        >
          已登记
        </DmButton>
        <DmButton
          variant="ghost"
          :class="{ active: sourceFilter === 'pending' }"
          @click="sourceFilter = 'pending'"
        >
          待上传
        </DmButton>
      </div>
      <div class="source-control-actions">
        <label class="source-search">
          <AppIcon name="search" aria-hidden="true" />
          <span class="dm-sr-only">搜索文档</span>
          <DmInput
            v-model="searchQuery"
            appearance="unstyled"
            type="search"
            placeholder="搜索文档名称"
          />
        </label>
        <DmButton
          class="source-refresh"
          variant="ghost"
          icon-only
          :disabled="isLoading"
          aria-label="刷新文档列表"
          @click="emit('reload')"
        >
          <AppIcon name="refresh" />
        </DmButton>
      </div>
    </div>

    <div class="source-list-actions">
      <span v-if="selectedSourceId">
        已选择：{{ sources.find((source) => source.id === selectedSourceId)?.name }}
      </span>
      <span v-else>选择文档后可查看任务信息</span>
      <span>当前载入 {{ sources.length }} 份</span>
    </div>

    <div class="source-table-head" aria-hidden="true">
      <span>文档名称</span><span>状态</span><span>更新时间</span><span>来源版本</span>
    </div>
    <div class="source-list-viewport">
      <InlineNotice v-if="loadError" tone="danger" title="列表加载失败" :detail="loadError" />
      <div v-if="isLoading" class="skeleton-list" aria-label="正在加载文档">
        <i v-for="placeholderNumber in 4" :key="placeholderNumber"></i>
      </div>
      <div v-else-if="sources.length === 0" class="empty-register">
        <span>00</span><strong>还没有原始文档</strong>
        <p>上传第一份文档后，将在这里显示版本与处理入口。</p>
      </div>
      <div v-else-if="filteredSources.length === 0" class="source-no-results">
        <strong>没有找到匹配的文档</strong>
        <p>试试更短的名称，或清除当前筛选条件。</p>
        <DmButton variant="accent-ghost" @click="emit('reset')">显示全部文档</DmButton>
      </div>
      <ol v-else class="source-list">
        <li v-for="source in pagedSources" :key="source.id">
          <DmInteractiveSurface
            class="source-row"
            :class="{ 'source-row--selected': selectedSourceId === source.id }"
            :aria-pressed="selectedSourceId === source.id"
            @click="emit('select', source)"
          >
            <span class="source-file-copy">
              <span class="file-glyph"><AppIcon name="document" /></span>
              <span class="source-name">
                <strong>{{ source.name }}</strong>
                <small>不可变原件</small>
              </span>
            </span>
            <DmStatus
              :label="source.current_version_id === null ? '待上传' : '已登记'"
              :tone="source.current_version_id === null ? 'warning' : 'success'"
            />
            <span class="source-updated-at">{{ formatSourceDate(source.updated_at) }}</span>
            <code>{{ getSourceVersionLabel(source) }}</code>
          </DmInteractiveSurface>
        </li>
      </ol>
    </div>

    <footer class="source-register-footer">
      <div class="source-list-summary" aria-live="polite">
        <span v-if="filteredSources.length">
          共 {{ filteredSources.length }} 条 · 显示 {{ pageStart }}–{{ pageEnd }}
        </span>
        <span v-else>没有匹配的文档</span>
        <DmButton
          v-if="searchQuery || sourceFilter !== 'all'"
          variant="accent-ghost"
          class="text-action"
          @click="emit('reset')"
        >
          清除筛选
        </DmButton>
      </div>
      <nav
        v-if="filteredSources.length > 0 && totalPages > 1"
        class="source-pagination"
        aria-label="文档列表分页"
      >
        <DmButton
          variant="secondary"
          aria-label="上一页"
          :disabled="safeCurrentPage === 1"
          @click="currentPage = safeCurrentPage - 1"
        >
          上一页
        </DmButton>
        <DmButton
          v-for="page in pageNumbers"
          :key="page"
          :variant="safeCurrentPage === page ? 'primary' : 'secondary'"
          :class="{ active: safeCurrentPage === page }"
          :aria-current="safeCurrentPage === page ? 'page' : undefined"
          :aria-label="`第 ${page} 页`"
          @click="currentPage = page"
        >
          {{ page }}
        </DmButton>
        <DmButton
          variant="secondary"
          aria-label="下一页"
          :disabled="safeCurrentPage === totalPages"
          @click="currentPage = safeCurrentPage + 1"
        >
          下一页
        </DmButton>
      </nav>
    </footer>
  </section>
</template>
