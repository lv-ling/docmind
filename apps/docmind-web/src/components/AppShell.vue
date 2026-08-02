<script setup lang="ts">
import { DmButton } from '@/ui';
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { RouteName } from '../router/constants.js';
import { useAuthStore } from '../stores/auth.js';
import { useWorkspaceStore } from '../stores/workspace.js';
import AppIcon from './AppIcon.vue';

const auth = useAuthStore();
const workspace = useWorkspaceStore();
const route = useRoute();
const router = useRouter();
const mobileNavigationOpen = ref(false);

const navigation = [
  {
    label: '原始文档',
    name: RouteName.SourceList,
    to: { name: RouteName.SourceList },
    icon: 'document' as const,
  },
  {
    label: '字段配置',
    name: RouteName.SchemaList,
    to: { name: RouteName.SchemaList },
    icon: 'schema' as const,
  },
  { label: '抽取复核', name: RouteName.ExtractionReview, to: null, icon: 'review' as const },
  {
    label: '文档模板',
    name: RouteName.TemplateList,
    to: { name: RouteName.TemplateList },
    icon: 'template' as const,
  },
];

const selectWorkspace = async (event: Event): Promise<void> => {
  const next = (event.target as HTMLSelectElement).value as import('@/contracts').WorkspaceId;
  workspace.select(next);
  await router.push({ name: RouteName.SourceList });
};

const logout = async (): Promise<void> => {
  auth.logout();
  workspace.reset();
  await router.replace({ name: RouteName.Login });
};

const isNavigationActive = (name: string): boolean => route.meta.menuKey === name;
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <button
        type="button"
        class="mobile-nav-trigger"
        aria-label="切换导航菜单"
        :aria-expanded="mobileNavigationOpen"
        @click="mobileNavigationOpen = !mobileNavigationOpen"
      >
        <span></span><span></span><span></span>
      </button>
      <RouterLink class="brand-lockup" :to="{ name: RouteName.SourceList }">
        <span class="brand-mark" aria-hidden="true"><i></i><i></i><i></i></span>
        <span><strong>DOCMIND</strong><small>文档智能工作台</small></span>
      </RouterLink>
      <label class="workspace-switcher">
        <span>工作区</span>
        <select :value="workspace.selectedId ?? ''" @change="selectWorkspace">
          <option v-for="item in workspace.workspaces" :key="item.id" :value="item.id">
            {{ item.name }}
          </option>
        </select>
      </label>
      <div class="header-user">
        <span class="user-avatar" aria-hidden="true">{{
          auth.user?.display_name.slice(0, 1)
        }}</span>
        <span class="user-copy"
          ><strong>{{ auth.user?.display_name }}</strong
          ><small>{{ auth.user?.email }}</small></span
        >
        <DmButton variant="ghost" size="small" aria-label="退出登录" @click="logout">
          <AppIcon name="logout" /><span class="desktop-only">退出</span>
        </DmButton>
      </div>
    </header>

    <aside class="app-sidebar" :class="{ 'app-sidebar--open': mobileNavigationOpen }">
      <p class="sidebar-kicker">工作台</p>
      <nav aria-label="主导航">
        <template v-for="item in navigation" :key="item.label">
          <RouterLink
            v-if="item.to !== null"
            class="sidebar-link"
            :class="{ 'sidebar-link--active': isNavigationActive(item.name) }"
            :to="item.to"
            @click="mobileNavigationOpen = false"
          >
            <AppIcon :name="item.icon" />
            <span>{{ item.label }}</span>
          </RouterLink>
          <span v-else class="sidebar-link sidebar-link--disabled" title="请从文档发起抽取">
            <AppIcon :name="item.icon" />
            <span>{{ item.label }}</span
            ><small>从文档进入</small>
          </span>
        </template>
      </nav>
      <div class="sidebar-footnote">
        <span class="security-pulse" aria-hidden="true"></span>
        <span><strong>敏感信息保护已开启</strong><small>发送模型前自动去标识化</small></span>
      </div>
    </aside>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>
