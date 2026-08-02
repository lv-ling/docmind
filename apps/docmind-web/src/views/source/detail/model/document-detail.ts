export interface DocumentVersionModel {
  id: string;
  label: string;
  fileName: string;
  date: string;
  status: '当前版本' | '历史版本';
}

export const DOCUMENT_DETAIL_MODEL = {
  id: 'source-contract-2023',
  name: '字节跳动2023年度采购框架协议.pdf',
  fileType: 'PDF',
  size: '3.8 MB',
  pages: 12,
  category: '商业合同',
  uploader: '林晓',
  uploadedAt: '2024-08-18 10:18',
  checksum: 'SHA-256 · 9FD2…77CA',
  template: '标准采购合同 v2',
  templateStatus: '已关联',
  processingRecords: [
    { label: '文档解析', detail: '版面分析与 OCR 识别完成', time: '10:19', status: 'done' },
    { label: '字段抽取', detail: '14 个字段已提取，平均置信度 92%', time: '10:21', status: 'done' },
    { label: '风险分析', detail: '发现 1 项违约金阈值风险', time: '10:23', status: 'warning' },
  ],
  versions: [
    {
      id: 'v3',
      label: 'V3',
      fileName: '字节跳动2023年度采购框架协议.pdf',
      date: '2024-08-18',
      status: '当前版本',
    },
    {
      id: 'v2',
      label: 'V2',
      fileName: '采购框架协议-法务修订版.pdf',
      date: '2024-08-16',
      status: '历史版本',
    },
    {
      id: 'v1',
      label: 'V1',
      fileName: '采购框架协议-初稿.pdf',
      date: '2024-08-12',
      status: '历史版本',
    },
  ] as readonly DocumentVersionModel[],
} as const;
