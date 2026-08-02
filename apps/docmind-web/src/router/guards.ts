import type { NavigationGuard, Router } from 'vue-router';

import { useAuthStore } from '../stores/auth.js';
import { useWorkspaceStore } from '../stores/workspace.js';
import { RouteName } from './constants.js';
import { getQueryString } from './query.js';
import { ROUTE_WHITE_LIST } from './whitelist.js';

const authGuard: NavigationGuard = async (to) => {
  const auth = useAuthStore();
  await auth.initialize();

  if (to.name === RouteName.Login && auth.isAuthenticated) {
    return { name: RouteName.SourceList };
  }

  if (to.name !== null && ROUTE_WHITE_LIST.has(to.name)) return true;

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth === true);
  if (requiresAuth && !auth.isAuthenticated) {
    return { name: RouteName.Login, query: { redirect: to.fullPath } };
  }

  return true;
};

const workspaceGuard: NavigationGuard = async (to) => {
  const requiresWorkspace = to.matched.some((record) => record.meta.requiresWorkspace === true);
  if (!requiresWorkspace || !useAuthStore().isAuthenticated) return true;

  await useWorkspaceStore().load();
  return true;
};

const queryGuard: NavigationGuard = (to) => {
  const requiredQuery = to.meta.requiredQuery ?? [];
  const hasMissingQuery = requiredQuery.some((key) => getQueryString(to.query[key]) === null);

  if (!hasMissingQuery) return true;
  return {
    name: to.meta.invalidQueryRedirect ?? RouteName.NotFound,
    replace: true,
  };
};

export const setupRouterGuards = (router: Router): void => {
  router.beforeEach(authGuard);
  router.beforeEach(workspaceGuard);
  router.beforeEach(queryGuard);

  router.afterEach((to) => {
    document.title = to.meta.title === undefined ? 'DocMind' : `${to.meta.title} | DocMind`;
  });
};
