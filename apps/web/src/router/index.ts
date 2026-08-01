import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router';

import AppShell from '../components/AppShell.vue';
import { useAuthStore } from '../stores/auth.js';
import { useWorkspaceStore } from '../stores/workspace.js';

const workspaceChildren = [
  {
    path: 'sources',
    name: 'sources',
    component: () => import('../views/SourcesView.vue'),
  },
  {
    path: 'sources/:sourceId',
    name: 'source-detail',
    component: () => import('../views/SourceDetailView.vue'),
  },
  {
    path: 'schemas',
    name: 'schemas',
    component: () => import('../views/SchemasView.vue'),
  },
  {
    path: 'extractions/new',
    name: 'extraction-new',
    component: () => import('../views/ExtractionCreateView.vue'),
  },
  {
    path: 'extractions/:extractionId',
    name: 'extraction-review',
    component: () => import('../views/ExtractionReviewView.vue'),
  },
  {
    path: 'templates',
    name: 'templates',
    component: () => import('../views/TemplatesView.vue'),
  },
  {
    path: 'templates/:templateId',
    name: 'template-editor',
    component: () => import('../views/TemplateEditorView.vue'),
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    {
      path: '/w/:workspaceId',
      component: AppShell,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: (route) => `/w/${String(route.params.workspaceId)}/sources` },
        ...workspaceChildren,
      ],
    },
    { path: '/', redirect: '/login' },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
});

const authenticatedTarget = async (
  to: RouteLocationNormalized,
): Promise<string | true | { name: string; query: Record<string, string> }> => {
  const auth = useAuthStore();
  await auth.initialize();

  if (!auth.isAuthenticated && to.meta.requiresAuth === true) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (auth.isAuthenticated && to.name === 'login') {
    const workspace = useWorkspaceStore();
    await workspace.load();
    return workspace.selectedId === null ? true : `/w/${workspace.selectedId}/sources`;
  }
  if (!auth.isAuthenticated) return true;

  const workspace = useWorkspaceStore();
  await workspace.load();
  if (to.path === '/') {
    return workspace.selectedId === null ? true : `/w/${workspace.selectedId}/sources`;
  }
  const routeWorkspaceId = to.params.workspaceId;
  if (typeof routeWorkspaceId === 'string') {
    const isMember = workspace.workspaces.some((item) => item.id === routeWorkspaceId);
    if (!isMember) {
      return workspace.selectedId === null ? true : `/w/${workspace.selectedId}/sources`;
    }
    workspace.select(routeWorkspaceId as import('@docmind/contracts').WorkspaceId);
  }
  return true;
};

router.beforeEach(authenticatedTarget);
