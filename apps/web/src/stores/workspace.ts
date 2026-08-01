import type { WorkspaceId, WorkspaceSummary } from '@docmind/contracts';
import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

import { listWorkspaces } from '../api/identity.js';

const WORKSPACE_KEY = 'docmind.workspace-id';

export const useWorkspaceStore = defineStore('workspace', () => {
  const workspaces = ref<WorkspaceSummary[]>([]);
  const selectedId = ref<WorkspaceId | null>(
    sessionStorage.getItem(WORKSPACE_KEY) as WorkspaceId | null,
  );
  const loaded = ref(false);
  const current = computed(
    () => workspaces.value.find((workspace) => workspace.id === selectedId.value) ?? null,
  );

  const load = async (): Promise<void> => {
    if (loaded.value) return;
    workspaces.value = await listWorkspaces();
    if (!workspaces.value.some((workspace) => workspace.id === selectedId.value)) {
      selectedId.value = workspaces.value[0]?.id ?? null;
    }
    if (selectedId.value !== null) sessionStorage.setItem(WORKSPACE_KEY, selectedId.value);
    loaded.value = true;
  };

  const select = (workspaceId: WorkspaceId): void => {
    if (!workspaces.value.some((workspace) => workspace.id === workspaceId)) return;
    selectedId.value = workspaceId;
    sessionStorage.setItem(WORKSPACE_KEY, workspaceId);
  };

  const reset = (): void => {
    workspaces.value = [];
    selectedId.value = null;
    loaded.value = false;
    sessionStorage.removeItem(WORKSPACE_KEY);
  };

  return { workspaces, selectedId, loaded, current, load, select, reset };
});
