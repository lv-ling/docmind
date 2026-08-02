export type ProcessingTab = 'running' | 'queued' | 'completed' | 'failed';
export type PipelineStageState = 'done' | 'active' | 'pending' | 'failed';

export interface ProcessingTask {
  id: string;
  title: string;
  scope: string;
  elapsed: string;
  status: string;
  progress: number;
  current: string;
  category: ProcessingTab;
  stages: readonly { label: string; state: PipelineStageState }[];
}

export const PROCESSING_TABS = [
  { value: 'running', label: '运行中', count: 2 },
  { value: 'queued', label: '排队中', count: 1 },
  { value: 'completed', label: '近期完成', count: 156 },
  { value: 'failed', label: '异常挂起', count: 1 },
] as const;

export const PROCESSING_TASKS: readonly ProcessingTask[] = [
  {
    id: 'T-88291',
    title: 'Q3季度发票批量抽取任务',
    scope: '包含 50 份文件',
    elapsed: '已运行 2m 14s',
    status: '正在解析实体',
    progress: 65,
    current: '当前: 提取第 32 份发票明细 (阿里云服务费202309.pdf)',
    category: 'running',
    stages: [
      { label: '文档解析', state: 'done' },
      { label: 'OCR识别', state: 'done' },
      { label: '字段抽取', state: 'active' },
      { label: '人工审核', state: 'pending' },
    ],
  },
  {
    id: 'T-88292',
    title: '跨境数据安全评估报告.pdf',
    scope: '单文件处理',
    elapsed: '已运行 45s',
    status: '敏感数据检测中',
    progress: 92,
    current: '当前: 匹配规则库 (Rule-02: PII Privacy)',
    category: 'running',
    stages: [
      { label: '文档解析', state: 'done' },
      { label: '语义分析', state: 'done' },
      { label: '敏感检测', state: 'active' },
      { label: '自动归档', state: 'pending' },
    ],
  },
  {
    id: 'T-88293',
    title: '供应商准入资料批量识别',
    scope: '包含 18 份文件',
    elapsed: '预计等待 1m',
    status: '等待可用处理资源',
    progress: 0,
    current: '队列位置: 1',
    category: 'queued',
    stages: [
      { label: '文档解析', state: 'pending' },
      { label: 'OCR识别', state: 'pending' },
      { label: '字段抽取', state: 'pending' },
      { label: '人工审核', state: 'pending' },
    ],
  },
  {
    id: 'T-88280',
    title: '八月员工入职材料',
    scope: '包含 24 份文件',
    elapsed: '用时 4m 38s',
    status: '处理完成',
    progress: 100,
    current: '已归档 24 份文档',
    category: 'completed',
    stages: [
      { label: '文档解析', state: 'done' },
      { label: '语义分析', state: 'done' },
      { label: '敏感检测', state: 'done' },
      { label: '自动归档', state: 'done' },
    ],
  },
  {
    id: 'T-88276',
    title: '历史合同补录任务',
    scope: '包含 70 份文件',
    elapsed: '挂起 12m',
    status: 'OCR 服务响应异常',
    progress: 28,
    current: '错误代码: OCR-UPSTREAM-TIMEOUT',
    category: 'failed',
    stages: [
      { label: '文档解析', state: 'done' },
      { label: 'OCR识别', state: 'failed' },
      { label: '字段抽取', state: 'pending' },
      { label: '人工审核', state: 'pending' },
    ],
  },
];
