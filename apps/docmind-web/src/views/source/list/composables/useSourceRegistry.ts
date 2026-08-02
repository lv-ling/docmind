import type { SourceDocument, WorkspaceId } from '@/contracts';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { listSources } from '@/api/sources.js';
import { RouteName } from '@/router/constants.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

import { filterSources, SOURCE_PAGE_SIZE, type SourceFilter } from '../model/source-list.js';

export const useSourceRegistry = () => {
  const route = useRoute();
  const router = useRouter();
  const workspace = useWorkspaceStore();
  const workspaceId = computed(() => workspace.selectedId as WorkspaceId);
  const sources = ref<SourceDocument[]>([]);
  const isLoadingSources = ref(true);
  const sourceLoadError = ref('');
  const searchQuery = ref('');
  const sourceFilter = ref<SourceFilter>('all');
  const currentPage = ref(1);
  const selectedSourceId = ref<string | null>(null);

  const filteredSources = computed(() =>
    filterSources(sources.value, searchQuery.value, sourceFilter.value),
  );
  const totalPages = computed(() =>
    Math.max(1, Math.ceil(filteredSources.value.length / SOURCE_PAGE_SIZE)),
  );
  const safeCurrentPage = computed(() => Math.min(currentPage.value, totalPages.value));
  const pagedSources = computed(() => {
    const start = (safeCurrentPage.value - 1) * SOURCE_PAGE_SIZE;
    return filteredSources.value.slice(start, start + SOURCE_PAGE_SIZE);
  });
  const pageStart = computed(() =>
    filteredSources.value.length === 0 ? 0 : (safeCurrentPage.value - 1) * SOURCE_PAGE_SIZE + 1,
  );
  const pageEnd = computed(() =>
    Math.min(safeCurrentPage.value * SOURCE_PAGE_SIZE, filteredSources.value.length),
  );
  const pageNumbers = computed(() =>
    Array.from({ length: totalPages.value }, (_, index) => index + 1),
  );
  const selectedSource = computed(
    () => sources.value.find((source) => source.id === selectedSourceId.value) ?? null,
  );

  const selectSource = (source: SourceDocument): void => {
    selectedSourceId.value = source.id;
  };

  const openSelectedSource = async (): Promise<void> => {
    if (selectedSource.value === null) return;
    await router.push({
      name: RouteName.SourceDetail,
      query: { sourceId: selectedSource.value.id },
    });
  };

  const startSelectedExtraction = async (): Promise<void> => {
    const versionId = selectedSource.value?.current_version_id;
    if (versionId === null || versionId === undefined) return;
    await router.push({
      name: RouteName.ExtractionCreate,
      query: { sourceVersionId: versionId },
    });
  };

  const resetFilters = (): void => {
    searchQuery.value = '';
    sourceFilter.value = 'all';
  };

  const loadSources = async (): Promise<void> => {
    isLoadingSources.value = true;
    sourceLoadError.value = '';
    try {
      const page = await listSources(workspaceId.value);
      sources.value = page.items;
      currentPage.value = 1;
      if (
        selectedSourceId.value === null ||
        !page.items.some((source) => source.id === selectedSourceId.value)
      ) {
        selectedSourceId.value = page.items[0]?.id ?? null;
      }
    } catch (caught) {
      sourceLoadError.value = caught instanceof Error ? caught.message : '文档列表加载失败';
    } finally {
      isLoadingSources.value = false;
    }
  };

  watch([searchQuery, sourceFilter], () => {
    currentPage.value = 1;
    const nextSources = filteredSources.value;
    if (!nextSources.some((source) => source.id === selectedSourceId.value)) {
      selectedSourceId.value = nextSources[0]?.id ?? null;
    }
  });
  watch(
    () => route.query.q,
    (value) => {
      searchQuery.value = typeof value === 'string' ? value : '';
    },
    { immediate: true },
  );
  watch(workspaceId, () => void loadSources());
  onMounted(loadSources);

  return {
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
  };
};
