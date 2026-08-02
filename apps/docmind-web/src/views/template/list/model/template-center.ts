export interface TemplateCenterItem {
  id: string;
  name: string;
  description: string;
  icon: 'layers' | 'receipt' | 'users';
  status: '启用中' | '灰度测试';
  fieldCount: number;
  usageCount: string;
  updatedAt: string;
}

export const TEMPLATE_CENTER_ITEMS: readonly TemplateCenterItem[] = [
  {
    id: 'template-purchase-v2',
    name: '标准采购合同 v2',
    description:
      '用于提取企业采购类框架协议及具体订单合同的核心商业条款与违约责任，关联风控法务库。',
    icon: 'layers',
    status: '启用中',
    fieldCount: 14,
    usageCount: '450 份文档',
    updatedAt: '今天 09:30',
  },
  {
    id: 'template-vat',
    name: '增值税发票通用引擎',
    description: '通用类财务票据抽取模型，支持多版式混排识别、明细行提取及跨页表格聚合。',
    icon: 'receipt',
    status: '启用中',
    fieldCount: 22,
    usageCount: '1,204 份文档',
    updatedAt: '昨天 18:12',
  },
  {
    id: 'template-nda',
    name: '员工保密及竞业协议',
    description: '专注于提取竞业限制周期、补偿金发放标准及违约条款，处于小流量验证阶段。',
    icon: 'users',
    status: '灰度测试',
    fieldCount: 6,
    usageCount: '12 份文档',
    updatedAt: '08-17 14:22',
  },
];
