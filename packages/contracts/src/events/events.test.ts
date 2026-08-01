import { describe, expect, expectTypeOf, it } from 'vitest';

import { DOMAIN_EVENT_TYPES, type DomainEvent } from '../index.js';

describe('event contracts', () => {
  it('publishes the current domain events', () => {
    expect(DOMAIN_EVENT_TYPES).toContain('instance.submitted');
    expect(DOMAIN_EVENT_TYPES).toContain('diff.completed');
  });

  it('requires CloudEvents metadata and an idempotency key', () => {
    expectTypeOf<DomainEvent>().toHaveProperty('specversion');
    expectTypeOf<DomainEvent>().toHaveProperty('idempotency_key');
  });

  it('does not permit arbitrary event payloads', () => {
    type ExtractionEvent = Extract<DomainEvent, { type: 'extraction.completed' }>;

    expectTypeOf<ExtractionEvent['data']>().toHaveProperty('extraction_run_id');
    expectTypeOf<ExtractionEvent['data']>().not.toHaveProperty('value');
    expectTypeOf<ExtractionEvent['data']>().not.toHaveProperty('text');
  });
});
