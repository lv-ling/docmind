<script setup lang="ts">
import type { SourceDocument } from '@/contracts';
import { DmDataTable, DmPagination, DmStatus, type DmDataTableColumn } from '@/ui';

import { AppIcon, InlineNotice } from '@/components/index.js';

import {
  formatSourceDate,
  getSourceVersionLabel,
  SOURCE_PAGE_SIZE,
  type SourceFilter,
} from '../../model/source-list.js';

defineOptions({ name: 'SourceRegister' });

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
}>();

const emit = defineEmits<{
  select: [source: SourceDocument];
  reset: [];
  reload: [];
}>();

const columns: DmDataTableColumn<SourceDocument>[] = [
  { key: 'name', header: '文档名称', accessor: (source) => source.name },
  {
    key: 'status',
    header: '状态',
    accessor: (source) => (source.current_version_id === null ? '待上传' : '已登记'),
    width: '7.5rem',
  },
  {
    key: 'updatedAt',
    header: '更新时间',
    accessor: (source) => formatSourceDate(source.updated_at),
    width: '9rem',
  },
  {
    key: 'version',
    header: '来源版本',
    accessor: getSourceVersionLabel,
    width: '7.5rem',
  },
];
</script>

<template>
  <section class="source-register" aria-label="文档登记簿">
    <div class="source-list-controls">
      <div class="source-tabs" role="group" aria-label="文档状态筛选">
        <button
          type="button"
          :class="{ active: sourceFilter === 'all' }"
          @click="sourceFilter = 'all'"
        >
          全部文档 <span>{{ sources.length }}</span>
        </button>
        <button
          type="button"
          :class="{ active: sourceFilter === 'registered' }"
          @click="sourceFilter = 'registered'"
        >
          已登记
        </button>
        <button
          type="button"
          :class="{ active: sourceFilter === 'pending' }"
          @click="sourceFilter = 'pending'"
        >
          待上传
        </button>
      </div>
      <div class="source-control-actions">
        <label class="source-search">
          <AppIcon name="search" aria-hidden="true" />
          <span class="dm-sr-only">搜索文档</span>
          <input v-model="searchQuery" type="search" placeholder="搜索文档名称" />
        </label>
        <button
          class="source-refresh"
          type="button"
          :disabled="isLoading"
          aria-label="刷新文档列表"
          @click="emit('reload')"
        >
          <AppIcon name="refresh" />
        </button>
      </div>
    </div>

    <div class="source-list-actions">
      <span v-if="selectedSourceId">
        已选择：{{ sources.find((source) => source.id === selectedSourceId)?.name }}
      </span>
      <span v-else>选择文档后可查看任务信息</span>
      <span>当前载入 {{ sources.length }} 份</span>
    </div>

    <div class="source-list-viewport">
      <InlineNotice v-if="loadError" tone="danger" title="列表加载失败" :detail="loadError" />
      <DmDataTable
        caption="文档登记簿"
        :columns="columns"
        :rows="pagedSources"
        :row-key="(source) => source.id"
        :is-loading="isLoading"
        :selected-row-key="selectedSourceId"
        :empty-title="sources.length === 0 ? '还没有原始文档' : '没有找到匹配的文档'"
        :empty-description="
          sources.length === 0
            ? '上传第一份文档后，将在这里显示版本与处理入口。'
            : '试试更短的名称，或清除当前筛选条件。'
        "
        is-row-interactive
        variant="plain"
        @row-click="emit('select', $event)"
      >
        <template #cell="{ row: source, column }">
          <template v-if="column.key === 'name'">
            <span class="source-file-copy">
              <span class="file-glyph"><AppIcon name="document" /></span>
              <span class="source-name">
                <strong>{{ source.name }}</strong>
                <small>不可变原件</small>
              </span>
            </span>
          </template>
          <DmStatus
            v-else-if="column.key === 'status'"
            :label="source.current_version_id === null ? '待上传' : '已登记'"
            :tone="source.current_version_id === null ? 'warning' : 'success'"
          />
          <span v-else-if="column.key === 'updatedAt'" class="source-updated-at">
            {{ formatSourceDate(source.updated_at) }}
          </span>
          <code v-else>{{ getSourceVersionLabel(source) }}</code>
        </template>
        <template #empty-icon>
          <AppIcon :name="sources.length === 0 ? 'document' : 'search'" />
        </template>
        <template v-if="sources.length > 0" #empty-actions>
          <button type="button" class="dm-state__action" @click="emit('reset')">
            显示全部文档
          </button>
        </template>
      </DmDataTable>
    </div>

    <footer class="source-register-footer">
      <DmPagination
        v-model="currentPage"
        :total-pages="totalPages"
        :total-items="filteredSources.length"
        :page-size="SOURCE_PAGE_SIZE"
        aria-label="文档列表分页"
      />
      <button
        v-if="searchQuery || sourceFilter !== 'all'"
        type="button"
        class="text-action"
        @click="emit('reset')"
      >
        清除筛选
      </button>
    </footer>
  </section>
</template>
