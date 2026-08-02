export const RouteName = {
  Login: 'Login',
  Workbench: 'Workbench',
  WorkbenchOverview: 'WorkbenchOverview',
  SourceList: 'SourceList',
  SourceDetail: 'SourceDetail',
  SchemaList: 'SchemaList',
  ExtractionCreate: 'ExtractionCreate',
  ExtractionProcessing: 'ExtractionProcessing',
  ExtractionReviewQueue: 'ExtractionReviewQueue',
  ExtractionReview: 'ExtractionReview',
  TemplateList: 'TemplateList',
  TemplateEditor: 'TemplateEditor',
  NotFound: 'NotFound',
} as const;

export const RoutePath = {
  Root: '/',
  Login: '/login',
  Workbench: '/workbench',
  NotFound: '/404',
} as const;
