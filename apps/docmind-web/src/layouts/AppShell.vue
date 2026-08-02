<script setup lang="ts">
import type { WorkspaceId } from '@/contracts';
import { computed, ref, watch } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';

import { RouteName } from '@/router/constants.js';
import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

import GlobalHeader from './components/GlobalHeader.vue';
import WorkspaceSidebar from './components/WorkspaceSidebar.vue';

const auth = useAuthStore();
const workspace = useWorkspaceStore();
const route = useRoute();
const router = useRouter();
const isMobileNavigationOpen = ref(false);
const isNotificationPanelOpen = ref(false);
const searchQuery = ref('');
const isImmersiveLayout = computed(() => route.meta.layout === 'immersive');

const handleWorkspaceChange = async (workspaceId: WorkspaceId): Promise<void> => {
  workspace.select(workspaceId);
  await router.push({ name: RouteName.WorkbenchOverview });
};

const handleGlobalSearch = async (): Promise<void> => {
  const query = searchQuery.value.trim();
  await router.push({
    name: RouteName.SourceList,
    query: query.length > 0 ? { q: query } : {},
  });
  isMobileNavigationOpen.value = false;
};

const handleCreateWorkflow = async (): Promise<void> => {
  await router.push({ name: RouteName.SourceList, query: { upload: '1' } });
  isMobileNavigationOpen.value = false;
};

const handleLogout = async (): Promise<void> => {
  auth.logout();
  workspace.reset();
  await router.replace({ name: RouteName.Login });
};

watch(
  () => route.fullPath,
  () => {
    isNotificationPanelOpen.value = false;
    isMobileNavigationOpen.value = false;
    if (route.name !== RouteName.SourceList) searchQuery.value = '';
  },
);
</script>

<template>
  <div class="workspace-shell" :class="{ 'workspace-shell--immersive': isImmersiveLayout }">
    <WorkspaceSidebar
      v-if="!isImmersiveLayout"
      :workspaces="workspace.workspaces"
      :selected-workspace-id="workspace.selectedId"
      :active-menu-key="typeof route.meta.menuKey === 'string' ? route.meta.menuKey : null"
      :active-hash="route.hash"
      :display-name="auth.user?.display_name ?? '用户'"
      :email="auth.user?.email ?? ''"
      :is-open="isMobileNavigationOpen"
      @change-workspace="handleWorkspaceChange"
      @navigate="isMobileNavigationOpen = false"
      @logout="handleLogout"
    />

    <div
      v-if="!isImmersiveLayout && isMobileNavigationOpen"
      class="workspace-sidebar-backdrop"
      aria-hidden="true"
      @click="isMobileNavigationOpen = false"
    ></div>

    <section class="workspace-content">
      <GlobalHeader
        v-model="searchQuery"
        :has-navigation="!isImmersiveLayout"
        :is-mobile-navigation-open="isMobileNavigationOpen"
        :is-notification-panel-open="isNotificationPanelOpen"
        @submit-search="handleGlobalSearch"
        @toggle-navigation="isMobileNavigationOpen = !isMobileNavigationOpen"
        @toggle-notifications="isNotificationPanelOpen = !isNotificationPanelOpen"
        @create-workflow="handleCreateWorkflow"
      />

      <main class="workspace-main">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
:global(body) {
  overflow: hidden;
  color: var(--dm-color-ink);
  background: var(--dm-color-canvas);
  font-family: var(--dm-font-ui);
  font-size: 13px;
}

.workspace-shell {
  display: grid;
  grid-template-columns: 215px minmax(0, 1fr);
  width: 100%;
  min-height: 100dvh;
  color: var(--dm-color-ink);
  background: var(--dm-color-paper);
}

.workspace-shell--immersive {
  grid-template-columns: minmax(0, 1fr);
}

.workspace-content {
  display: grid;
  grid-template-rows: 45px minmax(0, 1fr);
  min-width: 0;
  height: 100dvh;
  overflow: hidden;
}

.workspace-main {
  min-width: 0;
  min-height: 0;
  padding: 0;
  overflow: auto;
  background:
    radial-gradient(circle at 96% 0%, rgb(99 102 241 / 5%), transparent 38%), var(--dm-color-paper);
}

.workspace-sidebar-backdrop {
  display: none;
}

@media (max-width: 800px) {
  .workspace-shell {
    display: block;
  }

  .workspace-content {
    height: 100dvh;
  }

  .workspace-sidebar-backdrop {
    position: fixed;
    z-index: 35;
    inset: 0;
    display: block;
    background: rgb(24 24 27 / 34%);
    backdrop-filter: blur(2px);
  }
}
</style>
