import type { RouteRecordRaw } from 'vue-router';

import AppShell from '../../components/AppShell.vue';
import { RouteName, RoutePath } from '../constants.js';

const sourceRoutes: RouteRecordRaw[] = [
  {
    path: 'source/list',
    name: RouteName.SourceList,
    component: () => import('../../views/SourcesView.vue'),
    meta: {
      title: '原始文档',
      module: 'source',
      menuKey: RouteName.SourceList,
    },
  },
  {
    path: 'source/detail',
    name: RouteName.SourceDetail,
    component: () => import('../../views/SourceDetailView.vue'),
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
    component: () => import('../../views/SchemasView.vue'),
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
    component: () => import('../../views/ExtractionCreateView.vue'),
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
    component: () => import('../../views/ExtractionReviewView.vue'),
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
    component: () => import('../../views/TemplatesView.vue'),
    meta: {
      title: '文档模板',
      module: 'template',
      menuKey: RouteName.TemplateList,
    },
  },
  {
    path: 'template/editor',
    name: RouteName.TemplateEditor,
    component: () => import('../../views/TemplateEditorView.vue'),
    meta: {
      title: '模板编辑器',
      module: 'template',
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
  redirect: { name: RouteName.SourceList },
  meta: { requiresAuth: true, requiresWorkspace: true },
  children: [...sourceRoutes, ...schemaRoutes, ...extractionRoutes, ...templateRoutes],
};
