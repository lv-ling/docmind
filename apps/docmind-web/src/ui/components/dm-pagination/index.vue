<script setup lang="ts">
import { computed } from 'vue';

type PaginationItem = number | 'ellipsis-start' | 'ellipsis-end';

defineOptions({ name: 'DmPagination' });

const props = withDefaults(
  defineProps<{
    totalPages: number;
    totalItems?: number;
    pageSize?: number;
    ariaLabel?: string;
  }>(),
  {
    totalItems: 0,
    pageSize: 20,
    ariaLabel: '分页',
  },
);

const currentPage = defineModel<number>({ default: 1 });
const safePage = computed(() =>
  Math.min(Math.max(1, currentPage.value), Math.max(1, props.totalPages)),
);
const firstItem = computed(() =>
  props.totalItems === 0 ? 0 : (safePage.value - 1) * props.pageSize + 1,
);
const lastItem = computed(() => Math.min(props.totalItems, safePage.value * props.pageSize));
const paginationItems = computed<PaginationItem[]>(() => {
  if (props.totalPages <= 7)
    return Array.from({ length: props.totalPages }, (_, index) => index + 1);

  const items: PaginationItem[] = [1];
  const rangeStart = Math.max(2, safePage.value - 1);
  const rangeEnd = Math.min(props.totalPages - 1, safePage.value + 1);
  if (rangeStart > 2) items.push('ellipsis-start');
  for (let page = rangeStart; page <= rangeEnd; page += 1) items.push(page);
  if (rangeEnd < props.totalPages - 1) items.push('ellipsis-end');
  items.push(props.totalPages);
  return items;
});

const setPage = (page: number): void => {
  currentPage.value = Math.min(Math.max(1, page), Math.max(1, props.totalPages));
};
</script>

<template>
  <div class="dm-pagination">
    <p v-if="totalItems > 0">共 {{ totalItems }} 条 · 显示 {{ firstItem }}–{{ lastItem }}</p>
    <nav v-if="totalPages > 1" :aria-label="ariaLabel">
      <button
        type="button"
        :disabled="safePage === 1"
        aria-label="上一页"
        @click="setPage(safePage - 1)"
      >
        上一页
      </button>
      <template v-for="item in paginationItems" :key="item">
        <span v-if="typeof item !== 'number'" aria-hidden="true">…</span>
        <button
          v-else
          type="button"
          :aria-label="`第 ${item} 页`"
          :aria-current="safePage === item ? 'page' : undefined"
          @click="setPage(item)"
        >
          {{ item }}
        </button>
      </template>
      <button
        type="button"
        :disabled="safePage === totalPages"
        aria-label="下一页"
        @click="setPage(safePage + 1)"
      >
        下一页
      </button>
    </nav>
  </div>
</template>
