import type { ExtractionSchema, SourceDocumentPage, Template } from '@/contracts';

export interface WorkbenchBackendSummary {
  documentCount: number;
  hasMoreDocuments: boolean;
  activeConversionCount: number;
  readyTemplateCount: number;
  publishedSchemaCount: number;
  pendingSourceCount: number;
  failedConversionCount: number;
}

export interface WorkbenchActivitySummary {
  processedDocumentCount: number;
  attentionCount: number;
}

export interface WorkbenchExtractionField {
  label: string;
  value: string;
  isMonospace?: boolean;
}

export interface WorkbenchRiskNotice {
  title: string;
  beforeHighlight: string;
  highlightedValue: string;
  afterHighlight: string;
}

export interface WorkbenchAttentionItem {
  id: string;
  variant: 'review' | 'archive';
  title: string;
  category: string;
  confidenceLabel: string;
  updatedAt: string;
  recommendation?: string;
  actionLabel: string;
  extractionFields?: WorkbenchExtractionField[];
  riskNotice?: WorkbenchRiskNotice;
}

export interface WorkbenchPipelineItem {
  id: string;
  title: string;
  documentCount: number;
  stageLabel: string;
  progress: number;
}

export interface WorkbenchEfficiency {
  parsedDocumentCount: number;
  savedHours: number;
  accuracyRate: number;
  autoArchiveRate: number;
}

export interface WorkbenchInsight {
  title: string;
  description: string;
  actionLabel: string;
}

export interface WorkbenchOverview {
  backendSummary: WorkbenchBackendSummary;
  activitySummary: WorkbenchActivitySummary;
  attentionItems: WorkbenchAttentionItem[];
  pipelineItems: WorkbenchPipelineItem[];
  efficiency: WorkbenchEfficiency;
  insight: WorkbenchInsight;
}

const ACTIVE_CONVERSION_STATUSES = ['queued', 'running', 'retrying'] as const;

const FRONTEND_ACTIVITY_SUMMARY: WorkbenchActivitySummary = {
  processedDocumentCount: 12,
  attentionCount: 3,
};

const FRONTEND_ATTENTION_ITEMS: WorkbenchAttentionItem[] = [
  {
    id: 'frontend-review-contract',
    variant: 'review',
    title: '字节跳动2023年度采购框架协议.pdf',
    category: '商业合同',
    confidenceLabel: '整体置信度 92%',
    updatedAt: '10 分钟前',
    actionLabel: '进入复核',
    extractionFields: [
      { label: '总金额', value: '¥ 5,000,000', isMonospace: true },
      { label: '供应商 (乙方)', value: '上海微盟企业发展有限公司' },
      { label: '生效日期', value: '2023-01-01' },
    ],
    riskNotice: {
      title: 'AI 风险拦截：',
      beforeHighlight: '违约金比例 (',
      highlightedValue: '30%',
      afterHighlight: ') 超出企业最高阈值 (20%)，建议法务介入。',
    },
  },
  {
    id: 'frontend-review-employee',
    variant: 'archive',
    title: '员工保密及竞业限制协议-王五.docx',
    category: '人事合规',
    confidenceLabel: '字段全绿 (98%)',
    updatedAt: '刚刚',
    recommendation: '建议自动归档',
    actionLabel: '快捷通过',
  },
];

const FRONTEND_PIPELINE_ITEMS: WorkbenchPipelineItem[] = [
  {
    id: 'frontend-extraction-q3-invoice',
    title: 'Q3季度发票批量抽取任务',
    documentCount: 50,
    stageLabel: '实体抽取中...',
    progress: 65,
  },
];

const FRONTEND_EFFICIENCY: WorkbenchEfficiency = {
  parsedDocumentCount: 124,
  savedHours: 18,
  accuracyRate: 96.5,
  autoArchiveRate: 72,
};

const FRONTEND_INSIGHT: WorkbenchInsight = {
  title: '违约金条款异常高发',
  description: '近期扫描的 15 份合同中，有 4 份违约金超出 20% 阈值。',
  actionLabel: '调整全局抽取规则',
};

const createBackendSummary = (
  sourcePage: SourceDocumentPage,
  templates: Template[],
  schemas: ExtractionSchema[],
): WorkbenchBackendSummary => {
  const activeConversionCount = templates.filter((template) =>
    ACTIVE_CONVERSION_STATUSES.some((status) => status === template.conversion_status),
  ).length;

  return {
    documentCount: sourcePage.items.length,
    hasMoreDocuments: sourcePage.has_more,
    activeConversionCount,
    readyTemplateCount: templates.filter((template) => template.conversion_status === 'ready')
      .length,
    publishedSchemaCount: schemas.filter((schema) => schema.current_version_id !== null).length,
    pendingSourceCount: sourcePage.items.filter((source) => source.current_version_id === null)
      .length,
    failedConversionCount: templates.filter((template) => template.conversion_status === 'failed')
      .length,
  };
};

export const createEmptyWorkbenchOverview = (): WorkbenchOverview => ({
  backendSummary: {
    documentCount: 0,
    hasMoreDocuments: false,
    activeConversionCount: 0,
    readyTemplateCount: 0,
    publishedSchemaCount: 0,
    pendingSourceCount: 0,
    failedConversionCount: 0,
  },
  activitySummary: FRONTEND_ACTIVITY_SUMMARY,
  attentionItems: FRONTEND_ATTENTION_ITEMS,
  pipelineItems: FRONTEND_PIPELINE_ITEMS,
  efficiency: FRONTEND_EFFICIENCY,
  insight: FRONTEND_INSIGHT,
});

export const createWorkbenchOverview = (
  sourcePage: SourceDocumentPage,
  templates: Template[],
  schemas: ExtractionSchema[],
): WorkbenchOverview => ({
  ...createEmptyWorkbenchOverview(),
  backendSummary: createBackendSummary(sourcePage, templates, schemas),
});
