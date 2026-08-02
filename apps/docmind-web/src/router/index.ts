import './types.js';

import { createRouter, createWebHistory } from 'vue-router';

import { setupRouterGuards } from './guards.js';
import { appRoutes } from './modules/index.js';

export const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes,
  scrollBehavior: (to) =>
    to.hash.length > 0 ? { el: to.hash, top: 72, behavior: 'smooth' } : { top: 0 },
});

setupRouterGuards(router);

export { RouteName, RoutePath } from './constants.js';
