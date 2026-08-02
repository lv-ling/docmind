import type { RouteRecordRaw } from 'vue-router';

import { RouteName, RoutePath } from '../constants.js';
import { fallbackRoute, systemRoutes } from './system.js';
import { workbenchRoute } from './workbench.js';

export const appRoutes: RouteRecordRaw[] = [
  ...systemRoutes,
  {
    path: RoutePath.Root,
    redirect: { name: RouteName.WorkbenchOverview },
  },
  workbenchRoute,
  fallbackRoute,
];
