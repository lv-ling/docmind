import type { RouteRecordRaw } from 'vue-router';

import { RouteName } from '../constants.js';

export const workspaceRoutes: RouteRecordRaw[] = [
  {
    path: 'overview',
    name: RouteName.WorkbenchOverview,
    component: () => import('../../views/workbench/overview/index.vue'),
    meta: {
      title: '工作台',
      module: 'workspace',
      menuKey: RouteName.WorkbenchOverview,
    },
  },
];
