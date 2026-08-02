import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  DIFF_CHANGE_KINDS,
  DIFF_RUN_STATUSES,
  type CreateDiffRunRequest,
  type DiffRun,
} from '@/contracts';

describe('diff contracts', () => {
  it('models only persisted comparisons between immutable stored versions', () => {
    expect(DIFF_RUN_STATUSES).toEqual(['queued', 'parsing', 'comparing', 'completed', 'failed']);
    expect(DIFF_CHANGE_KINDS).toEqual(['insert', 'delete', 'replace', 'format', 'move']);
  });

  it('requires two typed document references for a canonical diff', () => {
    expectTypeOf<CreateDiffRunRequest>().toHaveProperty('baseline');
    expectTypeOf<CreateDiffRunRequest>().toHaveProperty('target');
    expectTypeOf<DiffRun>().toHaveProperty('algorithm_version');
    expectTypeOf<DiffRun['changes']>().toBeNullable();
  });
});
