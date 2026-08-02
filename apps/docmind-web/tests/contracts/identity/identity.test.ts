import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  MEMBER_STATUSES,
  USER_STATUSES,
  WORKSPACE_ROLES,
  type LoginResponse,
  type WorkspaceMember,
} from '@/contracts';

describe('identity contracts', () => {
  it('publishes finite user, member, and role states', () => {
    expect(USER_STATUSES).toEqual(['active', 'disabled']);
    expect(MEMBER_STATUSES).toEqual(['active', 'suspended']);
    expect(WORKSPACE_ROLES).toEqual(['owner', 'admin', 'editor', 'reviewer', 'viewer']);
  });

  it('keeps login and membership transport shapes explicit', () => {
    expectTypeOf<LoginResponse['token_type']>().toEqualTypeOf<'Bearer'>();
    expectTypeOf<WorkspaceMember['role']>().toEqualTypeOf<
      'owner' | 'admin' | 'editor' | 'reviewer' | 'viewer'
    >();
  });
});
