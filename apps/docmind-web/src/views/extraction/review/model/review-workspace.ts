export interface ReviewFieldModel {
  id: 'signDate' | 'partyA' | 'partyB' | 'amount' | 'penalty';
  label: string;
  value: string;
  confidence: number;
  risk?: boolean;
  suggestion?: string;
  evidence: string;
}

export const REVIEW_DOCUMENT = {
  title: '字节跳动2023年度采购框架协议.pdf',
  page: 1,
  pageCount: 12,
  documentType: '采购框架协议',
  parties: '2 家公司',
  amount: '¥ 5,000,000',
  subject: '技术服务',
} as const;

export const REVIEW_FIELDS: readonly ReviewFieldModel[] = [
  {
    id: 'signDate',
    label: '签署日期',
    value: '2023年1月1日',
    confidence: 97,
    evidence: '本协议由以下双方于 2023年1月1日 签署。',
  },
  {
    id: 'partyA',
    label: '甲方名称',
    value: '北京字节跳动科技有限公司',
    confidence: 99,
    evidence: '甲方：北京字节跳动科技有限公司',
  },
  {
    id: 'partyB',
    label: '乙方名称',
    value: '上海微盟企业发展有限公司',
    confidence: 98,
    evidence: '乙方：上海微盟企业发展有限公司',
  },
  {
    id: 'amount',
    label: '合同金额',
    value: '¥ 5,000,000.00',
    confidence: 96,
    evidence: '合同总金额预计为 ¥ 5,000,000.00 元。',
  },
  {
    id: 'penalty',
    label: '违约金比例',
    value: '30%',
    confidence: 72,
    risk: true,
    suggestion: '20%',
    evidence: '违约方应支付相当于违约金额 30% 的违约金。',
  },
];

export const REVIEW_PIPELINE = [
  { label: '文档识别', state: 'done' },
  { label: '字段抽取', state: 'done' },
  { label: '风险分析', state: 'active' },
] as const;
