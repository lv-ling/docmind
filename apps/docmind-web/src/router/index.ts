import './types.js';

import { createRouter, createWebHistory } from 'vue-router';

import { setupRouterGuards } from './guards.js';
import { appRoutes } from './modules/index.js';

export const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes,
  scrollBehavior: () => ({ top: 0 }),
});

setupRouterGuards(router);

export { RouteName, RoutePath } from './constants.js';
