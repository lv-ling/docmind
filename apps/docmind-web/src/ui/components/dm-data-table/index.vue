<script setup lang="ts" generic="TRow extends object">
import type { DmDataTableColumn, DmTableSort } from './data-table.js';
import DmEmptyState from '../dm-empty-state/index.vue';

defineOptions({ name: 'DmDataTable' });

const props = withDefaults(
  defineProps<{
    caption: string;
    columns: DmDataTableColumn<TRow>[];
    rows: TRow[];
    rowKey: (row: TRow) => string;
    sort?: DmTableSort | null;
    isLoading?: boolean;
    loadingRows?: number;
    emptyTitle?: string;
    emptyDescription?: string;
    isRowInteractive?: boolean;
    selectedRowKey?: string | null;
    variant?: 'card' | 'plain';
  }>(),
  {
    sort: null,
    isLoading: false,
    loadingRows: 5,
    emptyTitle: '暂无数据',
    emptyDescription: '调整筛选条件后再试。',
    isRowInteractive: false,
    selectedRowKey: null,
    variant: 'card',
  },
);

const emit = defineEmits<{
  sort: [sort: DmTableSort];
  'row-click': [row: TRow];
}>();

const formatCellValue = (value: unknown): string => {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'string' || typeof value === 'number') return String(value);
  if (typeof value === 'boolean') return value ? '是' : '否';
  return String(value);
};

const handleSort = (column: DmDataTableColumn<TRow>): void => {
  if (column.sortable !== true) return;
  const direction =
    props.sort?.key === column.key && props.sort.direction === 'asc' ? 'desc' : 'asc';
  emit('sort', { key: column.key, direction });
};

const handleRowClick = (row: TRow): void => {
  if (props.isRowInteractive) emit('row-click', row);
};

const handleRowKeydown = (event: KeyboardEvent, row: TRow): void => {
  if (!props.isRowInteractive || (event.key !== 'Enter' && event.key !== ' ')) return;
  event.preventDefault();
  emit('row-click', row);
};
</script>

<template>
  <div class="dm-data-table" :class="`dm-data-table--${variant}`">
    <div class="dm-data-table__viewport">
      <table>
        <caption class="dm-sr-only">
          {{
            caption
          }}
        </caption>
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              :class="`dm-data-table__cell--${column.align ?? 'left'}`"
              :style="column.width ? { width: column.width } : undefined"
              scope="col"
              :aria-sort="
                sort?.key === column.key
                  ? sort.direction === 'asc'
                    ? 'ascending'
                    : 'descending'
                  : undefined
              "
            >
              <button
                v-if="column.sortable"
                type="button"
                class="dm-data-table__sort"
                @click="handleSort(column)"
              >
                <span>{{ column.header }}</span>
                <span class="dm-data-table__sort-direction">{{
                  sort?.key === column.key ? (sort.direction === 'asc' ? '升序' : '降序') : '可排序'
                }}</span>
              </button>
              <span v-else>{{ column.header }}</span>
            </th>
          </tr>
        </thead>
        <tbody v-if="isLoading" aria-label="正在加载表格">
          <tr v-for="loadingRow in loadingRows" :key="loadingRow">
            <td v-for="column in columns" :key="column.key">
              <span class="dm-data-table__skeleton"></span>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="rows.length > 0">
          <tr
            v-for="row in rows"
            :key="rowKey(row)"
            :class="{
              'dm-data-table__row--interactive': isRowInteractive,
              'dm-data-table__row--selected': selectedRowKey === rowKey(row),
            }"
            :tabindex="isRowInteractive ? 0 : undefined"
            @click="handleRowClick(row)"
            @keydown="handleRowKeydown($event, row)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
              :class="`dm-data-table__cell--${column.align ?? 'left'}`"
            >
              <slot name="cell" :row="row" :column="column" :value="column.accessor(row)">
                {{ formatCellValue(column.accessor(row)) }}
              </slot>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr>
            <td :colspan="columns.length" class="dm-data-table__empty">
              <DmEmptyState :title="emptyTitle" :description="emptyDescription">
                <template v-if="$slots['empty-icon']" #icon><slot name="empty-icon" /></template>
                <template v-if="$slots['empty-actions']" #actions
                  ><slot name="empty-actions"
                /></template>
              </DmEmptyState>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
