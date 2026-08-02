import type { SourceDocument } from '@/contracts';

export type SourceFilter = 'all' | 'registered' | 'pending';

export type DocumentCategory = 'all' | 'success' | 'review';

export interface DocumentCenterItem {
  id: string;
  name: string;
  fileType: 'PDF' | 'DOCX' | 'XLSX';
  category: string;
  template: string;
  aiStatus: '处理成功' | '待审核' | '处理中';
  statusTone: 'success' | 'warning' | 'info';
  confidence: number;
  updatedAt: string;
  pages: number;
}

export const DOCUMENT_CENTER_ITEMS: readonly DocumentCenterItem[] = [
  {
    id: 'source-contract-2023',
    name: '字节跳动2023年度采购框架协议.pdf',
    fileType: 'PDF',
    category: '商业合同',
    template: '标准采购合同 v2',
    aiStatus: '待审核',
    statusTone: 'warning',
    confidence: 92,
    updatedAt: '今天 10:23',
    pages: 12,
  },
  {
    id: 'source-invoice-q3',
    name: 'Q3季度增值税发票汇总.pdf',
    fileType: 'PDF',
    category: '财务票据',
    template: '增值税发票通用引擎',
    aiStatus: '处理成功',
    statusTone: 'success',
    confidence: 99,
    updatedAt: '今天 09:46',
    pages: 48,
  },
  {
    id: 'source-nda-wang',
    name: '员工保密及竞业限制协议-王五.docx',
    fileType: 'DOCX',
    category: '人事合规',
    template: '员工保密及竞业协议',
    aiStatus: '处理成功',
    statusTone: 'success',
    confidence: 98,
    updatedAt: '今天 08:20',
    pages: 6,
  },
  {
    id: 'source-security-report',
    name: '跨境数据安全评估报告.pdf',
    fileType: 'PDF',
    category: '合规报告',
    template: '数据安全评估 v1',
    aiStatus: '处理中',
    statusTone: 'info',
    confidence: 86,
    updatedAt: '昨天 18:32',
    pages: 31,
  },
];

export const DOCUMENT_CATEGORY_TABS = [
  { value: 'all', label: '全部文档', count: 1204 },
  { value: 'success', label: '处理成功', count: 1150 },
  { value: 'review', label: '待审核', count: 3 },
] as const;

export const filterDocumentCenterItems = (
  items: readonly DocumentCenterItem[],
  query: string,
  category: DocumentCategory,
  fileType: string,
): DocumentCenterItem[] => {
  const normalized = query.trim().toLocaleLowerCase('zh-CN');
  return items.filter((item) => {
    const matchesQuery =
      normalized.length === 0 ||
      [item.name, item.category, item.template].some((value) =>
        value.toLocaleLowerCase('zh-CN').includes(normalized),
      );
    const matchesCategory =
      category === 'all' ||
      (category === 'success' && item.aiStatus === '处理成功') ||
      (category === 'review' && item.aiStatus === '待审核');
    const matchesType = fileType === 'all' || item.fileType === fileType;
    return matchesQuery && matchesCategory && matchesType;
  });
};

export const SOURCE_PAGE_SIZE = 6;

export const filterSources = (
  sources: SourceDocument[],
  searchQuery: string,
  sourceFilter: SourceFilter,
): SourceDocument[] => {
  const query = searchQuery.trim().toLocaleLowerCase('zh-CN');
  return sources.filter((source) => {
    const matchesQuery =
      query.length === 0 || source.name.toLocaleLowerCase('zh-CN').includes(query);
    const matchesFilter =
      sourceFilter === 'all' ||
      (sourceFilter === 'registered' && source.current_version_id !== null) ||
      (sourceFilter === 'pending' && source.current_version_id === null);
    return matchesQuery && matchesFilter;
  });
};

export const formatSourceDate = (value: string): string =>
  new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });

export const getSourceVersionLabel = (source: SourceDocument): string =>
  source.current_version_id?.slice(0, 8) ?? '待上传';
