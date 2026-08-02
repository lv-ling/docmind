export type ConfigTab = 'schema' | 'sensitive';

export interface SchemaFieldConfig {
  id: string;
  name: string;
  key: string;
  type: '文本' | '金额' | '日期' | '百分比';
  required: boolean;
}

export const CONFIG_TABS = [
  { value: 'schema', label: 'Schema 管理', count: 3 },
  { value: 'sensitive', label: '敏感规则', count: 3 },
] as const;

export const SCHEMA_LIST = [
  {
    id: 'schema-purchase',
    name: '采购合同 Schema',
    version: 'v2.4',
    fieldCount: 14,
    status: '已发布',
  },
  {
    id: 'schema-invoice',
    name: '增值税发票 Schema',
    version: 'v3.1',
    fieldCount: 22,
    status: '已发布',
  },
  { id: 'schema-nda', name: '竞业协议 Schema', version: 'v0.8', fieldCount: 6, status: '草稿' },
] as const;

export const DEFAULT_SCHEMA_FIELDS: readonly SchemaFieldConfig[] = [
  { id: 'field-1', name: '甲方名称', key: 'party_a.name', type: '文本', required: true },
  { id: 'field-2', name: '乙方名称', key: 'party_b.name', type: '文本', required: true },
  { id: 'field-3', name: '合同金额', key: 'contract.amount', type: '金额', required: true },
  { id: 'field-4', name: '签署日期', key: 'contract.signed_at', type: '日期', required: true },
  { id: 'field-5', name: '违约金比例', key: 'risk.penalty_rate', type: '百分比', required: false },
];

export const SENSITIVE_RULES = [
  {
    id: 'pii',
    name: '个人隐私数据 (PII)',
    description: '识别中国大陆二代身份证号、护照号、手机号码等高敏身份信息。',
    enabled: true,
  },
  {
    id: 'finance',
    name: '银行与财务凭证账号',
    description: '拦截对公银行账号、SWIFT 代码、信用卡号，防范资金流向信息泄露。',
    enabled: true,
  },
  {
    id: 'confidential',
    name: '内部机密字样感知 (Confidential)',
    description: '识别文档中的“绝密”、“机密”、“Confidential”等水印或页眉。',
    enabled: false,
  },
] as const;
