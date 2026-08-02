import type { WorkspaceId } from '@/contracts';
import { computed, onMounted, ref, watch } from 'vue';

import { listSchemas } from '@/api/schemas.js';
import { listSources } from '@/api/sources.js';
import { listTemplates } from '@/api/templates.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

import {
  createEmptyWorkbenchOverview,
  createWorkbenchOverview,
} from '../model/workbench-overview.js';

export const useWorkbenchOverview = () => {
  const workspace = useWorkspaceStore();
  const workspaceId = computed(() => workspace.selectedId as WorkspaceId);
  const overview = ref(createEmptyWorkbenchOverview());
  const isLoadingOverview = ref(true);
  const overviewError = ref('');

  const loadOverview = async (): Promise<void> => {
    isLoadingOverview.value = true;
    overviewError.value = '';
    try {
      const [sourcePage, templates, schemas] = await Promise.all([
        listSources(workspaceId.value),
        listTemplates(workspaceId.value),
        listSchemas(workspaceId.value),
      ]);
      overview.value = createWorkbenchOverview(sourcePage, templates, schemas);
    } catch (caught) {
      overviewError.value = caught instanceof Error ? caught.message : '工作台数据加载失败';
    } finally {
      isLoadingOverview.value = false;
    }
  };

  watch(workspaceId, () => void loadOverview());
  onMounted(loadOverview);

  return { overview, isLoadingOverview, overviewError, loadOverview };
};
