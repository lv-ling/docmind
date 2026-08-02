import type { RouteRecordRaw } from 'vue-router';

import AppShell from '@/layouts/AppShell.vue';

import { RouteName, RoutePath } from '../constants.js';

const workspaceRoutes: RouteRecordRaw[] = [
  {
    path: 'overview',
    name: RouteName.WorkbenchOverview,
    component: () => import('@/views/workbench/overview/index.vue'),
    meta: {
      title: '工作台',
      module: 'workspace',
      menuKey: RouteName.WorkbenchOverview,
    },
  },
];

const sourceRoutes: RouteRecordRaw[] = [
  {
    path: 'source/list',
    name: RouteName.SourceList,
    component: () => import('@/views/source/list/index.vue'),
    meta: {
      title: '原始文档',
      module: 'source',
      menuKey: RouteName.SourceList,
    },
  },
  {
    path: 'source/detail',
    name: RouteName.SourceDetail,
    component: () => import('@/views/source/detail/index.vue'),
    meta: {
      title: '文档详情',
      module: 'source',
      menuKey: RouteName.SourceList,
      hidden: true,
      requiredQuery: ['sourceId'],
      invalidQueryRedirect: RouteName.SourceList,
    },
  },
];

const schemaRoutes: RouteRecordRaw[] = [
  {
    path: 'schema/list',
    name: RouteName.SchemaList,
    component: () => import('@/views/schema/list/index.vue'),
    meta: {
      title: '字段配置',
      module: 'schema',
      menuKey: RouteName.SchemaList,
    },
  },
];

const extractionRoutes: RouteRecordRaw[] = [
  {
    path: 'extraction/create',
    name: RouteName.ExtractionCreate,
    component: () => import('@/views/extraction/create/index.vue'),
    meta: {
      title: '发起字段抽取',
      module: 'extraction',
      menuKey: RouteName.ExtractionReview,
      hidden: true,
      requiredQuery: ['sourceVersionId'],
      invalidQueryRedirect: RouteName.SourceList,
    },
  },
  {
    path: 'extraction/review',
    name: RouteName.ExtractionReview,
    component: () => import('@/views/extraction/review/index.vue'),
    meta: {
      title: '抽取复核',
      module: 'extraction',
      menuKey: RouteName.ExtractionReview,
      hidden: true,
      requiredQuery: ['extractionId'],
      invalidQueryRedirect: RouteName.SourceList,
    },
  },
];

const templateRoutes: RouteRecordRaw[] = [
  {
    path: 'template/list',
    name: RouteName.TemplateList,
    component: () => import('@/views/template/list/index.vue'),
    meta: {
      title: '文档模板',
      module: 'template',
      menuKey: RouteName.TemplateList,
    },
  },
  {
    path: 'template/editor',
    name: RouteName.TemplateEditor,
    component: () => import('@/views/template/editor/index.vue'),
    meta: {
      title: '模板编辑器',
      module: 'template',
      layout: 'immersive',
      menuKey: RouteName.TemplateList,
      hidden: true,
      requiredQuery: ['templateId'],
      invalidQueryRedirect: RouteName.TemplateList,
    },
  },
];

export const workbenchRoute: RouteRecordRaw = {
  path: RoutePath.Workbench,
  name: RouteName.Workbench,
  component: AppShell,
  redirect: { name: RouteName.WorkbenchOverview },
  meta: { requiresAuth: true, requiresWorkspace: true },
  children: [
    ...workspaceRoutes,
    ...sourceRoutes,
    ...schemaRoutes,
    ...extractionRoutes,
    ...templateRoutes,
  ],
};
