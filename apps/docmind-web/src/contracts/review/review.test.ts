import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  COMMENT_THREAD_STATUSES,
  PROOFREADING_CATEGORIES,
  PROOFREADING_SUGGESTION_STATUSES,
  type CommentThread,
  type CreateCommentThreadRequest,
  type ProofreadingSuggestion,
} from '../index.js';

describe('review contracts', () => {
  it('models reviewable comments and non-automatic proofreading outcomes', () => {
    expect(COMMENT_THREAD_STATUSES).toEqual(['open', 'resolved']);
    expect(PROOFREADING_CATEGORIES).toContain('grammar');
    expect(PROOFREADING_SUGGESTION_STATUSES).toEqual(['open', 'accepted', 'dismissed']);
  });

  it('anchors comments and suggestions with quote context', () => {
    expectTypeOf<CommentThread['anchor']['quote']>().toBeString();
    expectTypeOf<ProofreadingSuggestion['anchor']['prefix']>().toBeString();
    expectTypeOf<CreateCommentThreadRequest>().not.toHaveProperty('instance_version_id');
    expectTypeOf<ProofreadingSuggestion>().toHaveProperty('applied_instance_version_id');
    expectTypeOf<ProofreadingSuggestion['resolution_reason']>().toBeNullable();
  });
});
