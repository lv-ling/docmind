<script setup lang="ts">
import type { WorkspaceId, WorkspaceSummary } from '@/contracts';
import { RouterLink, type RouteLocationRaw } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import { RouteName } from '@/router/constants.js';

interface NavigationItem {
  label: string;
  icon: InstanceType<typeof AppIcon>['$props']['name'];
  routeName: (typeof RouteName)[keyof typeof RouteName];
  hash?: string;
  badge?: string;
}

interface NavigationGroup {
  label: string;
  items: NavigationItem[];
}

const props = defineProps<{
  workspaces: WorkspaceSummary[];
  selectedWorkspaceId: WorkspaceId | null;
  activeMenuKey: string | null;
  activeHash: string;
  displayName: string;
  email: string;
  isOpen: boolean;
}>();

const emit = defineEmits<{
  'change-workspace': [workspaceId: WorkspaceId];
  navigate: [];
  logout: [];
}>();

const NAVIGATION_GROUPS: NavigationGroup[] = [
  {
    label: '智能协作流程',
    items: [
      { label: '工作台', icon: 'grid', routeName: RouteName.WorkbenchOverview },
      { label: '文档中心', icon: 'documents', routeName: RouteName.SourceList },
      {
        label: 'AI 处理中心',
        icon: 'cpu',
        routeName: RouteName.WorkbenchOverview,
        hash: '#workbench-pipeline',
      },
      {
        label: '审核中心',
        icon: 'review',
        routeName: RouteName.WorkbenchOverview,
        hash: '#workbench-attention',
        badge: '3',
      },
    ],
  },
  {
    label: '规则与知识',
    items: [
      { label: '抽取模板', icon: 'template', routeName: RouteName.TemplateList },
      { label: '配置中心', icon: 'settings', routeName: RouteName.SchemaList },
    ],
  },
];

const isNavigationActive = (item: NavigationItem): boolean => {
  if (item.hash !== undefined) return props.activeHash === item.hash;
  if (item.routeName === RouteName.WorkbenchOverview) {
    return props.activeMenuKey === item.routeName && props.activeHash.length === 0;
  }
  return props.activeMenuKey === item.routeName;
};

const getNavigationTarget = (item: NavigationItem): RouteLocationRaw =>
  item.hash === undefined ? { name: item.routeName } : { name: item.routeName, hash: item.hash };

const handleWorkspaceChange = (event: Event): void => {
  emit('change-workspace', (event.target as HTMLSelectElement).value as WorkspaceId);
};
</script>

<template>
  <aside
    id="workspace-navigation"
    class="workspace-sidebar"
    :class="{ 'workspace-sidebar--open': isOpen }"
  >
    <label class="sidebar-workspace-switcher">
      <span class="workspace-mark" aria-hidden="true"><AppIcon name="bot" /></span>
      <span class="dm-sr-only">当前工作区</span>
      <select :value="selectedWorkspaceId ?? ''" @change="handleWorkspaceChange">
        <option v-for="workspace in workspaces" :key="workspace.id" :value="workspace.id">
          {{ workspace.name }}
        </option>
      </select>
      <AppIcon name="chevrons" aria-hidden="true" />
    </label>

    <div class="workspace-navigation">
      <section v-for="group in NAVIGATION_GROUPS" :key="group.label">
        <h2>{{ group.label }}</h2>
        <nav :aria-label="group.label">
          <RouterLink
            v-for="item in group.items"
            :key="item.label"
            class="workspace-navigation__item"
            :class="{ 'workspace-navigation__item--active': isNavigationActive(item) }"
            :to="getNavigationTarget(item)"
            @click="emit('navigate')"
          >
            <AppIcon :name="item.icon" />
            <span>{{ item.label }}</span>
            <span v-if="item.badge" class="workspace-navigation__badge">{{ item.badge }}</span>
          </RouterLink>
        </nav>
      </section>
    </div>

    <footer class="workspace-sidebar__footer">
      <div class="ai-connection-status">
        <span aria-hidden="true"></span>
        <strong>AI 引擎监控中</strong>
      </div>
      <div class="workspace-user">
        <span class="workspace-user__avatar" aria-hidden="true">
          {{ displayName.trim().slice(0, 1) || 'U' }}
        </span>
        <strong :title="email">{{ displayName }}</strong>
        <button type="button" aria-label="退出登录" title="退出登录" @click="emit('logout')">
          <AppIcon name="logout" />
        </button>
      </div>
    </footer>
  </aside>
</template>

<style scoped src="./workspace-sidebar.css"></style>
