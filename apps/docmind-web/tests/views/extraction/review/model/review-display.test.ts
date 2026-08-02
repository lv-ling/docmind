import type { ExtractionFieldResultView } from '@/contracts';
import { describe, expect, it } from 'vitest';

import {
  getConfidenceLabel,
  getExtractionDisplayValueText,
  getExtractionStatusLabel,
  getExtractionStatusTone,
  getExtractionValueBadge,
} from '@/views/extraction/review/model/review-display.js';

describe('extraction review display model', () => {
  it('maps run statuses to readable labels and tones', () => {
    expect(getExtractionStatusLabel('review_required')).toBe('待人工复核');
    expect(getExtractionStatusTone('review_required')).toBe('warning');
    expect(getExtractionStatusTone('approved')).toBe('success');
    expect(getExtractionStatusTone('failed')).toBe('danger');
  });

  it('formats visible and masked display values', () => {
    expect(getExtractionDisplayValueText({ access: 'visible', value: { total: 8 } })).toBe(
      '{\n  "total": 8\n}',
    );
    expect(
      getExtractionDisplayValueText({ access: 'masked', value: null, masked_preview: '***1234' }),
    ).toBe('***1234');
  });

  it('describes missing, default, and masked field values', () => {
    const createField = (
      overrides: Partial<ExtractionFieldResultView>,
    ): ExtractionFieldResultView =>
      ({
        display_value: { access: 'visible', value: null },
        missing_reason: null,
        value_source: 'model',
        ...overrides,
      }) as ExtractionFieldResultView;

    expect(getExtractionValueBadge(createField({}))).toBe('NULL');
    expect(getExtractionValueBadge(createField({ missing_reason: 'not_found' }))).toBe(
      '缺失 · not_found',
    );
    expect(
      getExtractionValueBadge(
        createField({ display_value: { access: 'masked', value: null, masked_preview: '***' } }),
      ),
    ).toBe('已脱敏');
  });

  it('formats valid confidence values and rejects invalid ones', () => {
    expect(getConfidenceLabel(0.876)).toBe('88%');
    expect(getConfidenceLabel(null)).toBe('—');
    expect(getConfidenceLabel(Number.NaN)).toBe('—');
  });
});
