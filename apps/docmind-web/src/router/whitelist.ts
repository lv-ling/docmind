import type { RouteRecordName } from 'vue-router';

import { RouteName } from './constants.js';

export const ROUTE_WHITE_LIST = new Set<RouteRecordName>([RouteName.Login, RouteName.NotFound]);
