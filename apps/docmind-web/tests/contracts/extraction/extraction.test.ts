import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  EXTRACTION_MISSING_REASONS,
  EXTRACTION_RUN_STATUSES,
  EXTRACTION_VALUE_SOURCES,
  type AcceptedExtractionJob,
  type ExtractionFieldResult,
  type ExtractionFieldResultView,
  type ExtractionResult,
  type ExtractionResultView,
  type ExtractionRun,
  type ExtractionRunView,
} from '@/contracts';

describe('extraction contracts', () => {
  it('publishes the review-aware extraction lifecycle', () => {
    expect(EXTRACTION_RUN_STATUSES).toEqual([
      'queued',
      'running',
      'review_required',
      'approved',
      'failed',
      'retrying',
    ]);
    expect(EXTRACTION_VALUE_SOURCES).toContain('null');
    expect(EXTRACTION_MISSING_REASONS).toContain('ambiguous');
  });

  it('keeps field results traceable and candidate-aware', () => {
    expectTypeOf<ExtractionFieldResult>().toHaveProperty('evidence');
    expectTypeOf<ExtractionFieldResult>().toHaveProperty('candidates');
    expectTypeOf<ExtractionFieldResult['reviewed_value']>().toBeNullable();
    expectTypeOf<ExtractionResult['data']>().toMatchTypeOf<Record<string, unknown>>();
  });

  it('separates persisted plaintext from permission-filtered Web views', () => {
    expectTypeOf<ExtractionFieldResultView>().not.toHaveProperty('value');
    expectTypeOf<ExtractionFieldResultView>().toHaveProperty('display_value');
    expectTypeOf<ExtractionResultView>().toHaveProperty('contains_masked_values');
    expectTypeOf<ExtractionRunView['result']>().toEqualTypeOf<ExtractionResultView | null>();
  });

  it('returns both task and extraction correlation identifiers', () => {
    expectTypeOf<AcceptedExtractionJob>().toHaveProperty('job_id');
    expectTypeOf<AcceptedExtractionJob>().toHaveProperty('extraction_id');
    expectTypeOf<ExtractionRun>().toHaveProperty('job_id');
  });
});
