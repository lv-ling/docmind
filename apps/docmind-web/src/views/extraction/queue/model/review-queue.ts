export interface ReviewQueueItem {
  id: string;
  title: string;
  confidence: number;
  category: string;
  updatedAt: string;
  riskLevel: 'high' | 'normal';
  detail: string;
  suggestion: string;
}

export const REVIEW_QUEUE_ITEMS: readonly ReviewQueueItem[] = [
  {
    id: 'extraction-contract-2023',
    title: '字节跳动2023年度采购框架协议.pdf',
    confidence: 92,
    category: '商业合同',
    updatedAt: '10 分钟前',
    riskLevel: 'high',
    detail: '风险原因：违约金比例 (30%) 超出企业最高阈值 (20%)',
    suggestion: 'AI 建议：修正并确认',
  },
  {
    id: 'extraction-security',
    title: '跨境数据安全评估报告.pdf',
    confidence: 89,
    category: '数据合规',
    updatedAt: '28 分钟前',
    riskLevel: 'high',
    detail: '风险原因：发现 2 处未脱敏个人身份信息，需要确认流转范围',
    suggestion: 'AI 建议：脱敏后归档',
  },
  {
    id: 'extraction-nda-wang',
    title: '员工保密及竞业限制协议-王五.docx',
    confidence: 98,
    category: '人事合规',
    updatedAt: '1 小时前',
    riskLevel: 'normal',
    detail: '状态：核心字段全部提取成功，未发现合规风险',
    suggestion: 'AI 建议：快捷归档',
  },
];
