import type { RouteRecordRaw } from 'vue-router';

import { RouteName, RoutePath } from '../constants.js';

export const systemRoutes: RouteRecordRaw[] = [
  {
    path: RoutePath.Login,
    name: RouteName.Login,
    component: () => import('../../views/LoginView.vue'),
    meta: { title: '登录', module: 'system' },
  },
  {
    path: RoutePath.NotFound,
    name: RouteName.NotFound,
    component: () => import('../../views/NotFoundView.vue'),
    meta: { title: '页面不存在', module: 'system' },
  },
];

export const fallbackRoute: RouteRecordRaw = {
  path: '/:pathMatch(.*)*',
  redirect: { name: RouteName.NotFound },
};
