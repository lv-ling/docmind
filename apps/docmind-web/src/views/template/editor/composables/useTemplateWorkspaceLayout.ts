import { computed, onMounted, ref, watch } from 'vue';

import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

export const useTemplateWorkspaceLayout = () => {
  const auth = useAuthStore();
  const workspace = useWorkspaceStore();
  const splitPercentage = ref(48);
  const isLeftPanelCollapsed = ref(false);
  const isRightPanelCollapsed = ref(false);
  const zoomPercentage = ref(90);
  const originalPage = ref(1);

  const layoutStorageKey = computed(
    () =>
      `docmind.template-split.${auth.user?.id ?? 'anonymous'}.${workspace.selectedId ?? 'none'}`,
  );

  const restoreLayout = (): void => {
    try {
      const stored = localStorage.getItem(layoutStorageKey.value);
      if (stored === null) return;
      const value = JSON.parse(stored) as { split?: unknown; left?: unknown; right?: unknown };
      if (typeof value.split === 'number') splitPercentage.value = value.split;
      if (typeof value.left === 'boolean') isLeftPanelCollapsed.value = value.left;
      if (typeof value.right === 'boolean') isRightPanelCollapsed.value = value.right;
    } catch {
      localStorage.removeItem(layoutStorageKey.value);
    }
  };

  watch([splitPercentage, isLeftPanelCollapsed, isRightPanelCollapsed], () => {
    localStorage.setItem(
      layoutStorageKey.value,
      JSON.stringify({
        split: splitPercentage.value,
        left: isLeftPanelCollapsed.value,
        right: isRightPanelCollapsed.value,
      }),
    );
  });
  onMounted(restoreLayout);

  return {
    splitPercentage,
    isLeftPanelCollapsed,
    isRightPanelCollapsed,
    zoomPercentage,
    originalPage,
  };
};
