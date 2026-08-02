import 'vue-router';

import type { RouteRecordName } from 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    requiresAuth?: boolean;
    requiresWorkspace?: boolean;
    layout?: 'default' | 'immersive';
    module?: 'workspace' | 'source' | 'schema' | 'extraction' | 'template' | 'system';
    menuKey?: RouteRecordName;
    hidden?: boolean;
    requiredQuery?: readonly string[];
    invalidQueryRedirect?: RouteRecordName;
  }
}

export {};
