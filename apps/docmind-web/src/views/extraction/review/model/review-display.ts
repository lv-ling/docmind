import type {
  ExtractionDisplayValue,
  ExtractionFieldResultView,
  ExtractionRunStatus,
} from '@/contracts';

export const getExtractionStatusLabel = (status: ExtractionRunStatus): string =>
  ({
    queued: '排队中',
    running: '识别中',
    retrying: '正在重试',
    review_required: '待人工复核',
    approved: '已批准',
    failed: '处理失败',
  })[status];

export const getExtractionStatusTone = (
  status: ExtractionRunStatus,
): 'info' | 'success' | 'warning' | 'danger' => {
  if (status === 'approved') return 'success';
  if (status === 'failed') return 'danger';
  if (status === 'review_required') return 'warning';
  return 'info';
};

export const getExtractionDisplayValueText = (display: ExtractionDisplayValue): string => {
  if (display.access === 'masked') return display.masked_preview;
  if (display.value === null) return 'null';
  if (display.value === '') return '空字符串 ""';
  if (typeof display.value === 'string') return display.value;
  return JSON.stringify(display.value, null, 2);
};

export const getExtractionValueBadge = (field: ExtractionFieldResultView): string => {
  if (field.display_value.access === 'masked') return '已脱敏';
  if (field.display_value.value === null) {
    return field.missing_reason === null ? 'NULL' : `缺失 · ${field.missing_reason}`;
  }
  if (field.display_value.value === '') return '空字符串';
  return field.value_source === 'default' ? '默认值' : field.value_source;
};

export const getConfidenceLabel = (confidence: number | null | undefined): string =>
  confidence == null || !Number.isFinite(confidence) ? '—' : `${Math.round(confidence * 100)}%`;
