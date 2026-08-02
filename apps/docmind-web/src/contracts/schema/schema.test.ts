import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  FIELD_SENSITIVITY_LEVELS,
  SCHEMA_VALUE_TYPES,
  SCHEMA_VERSION_STATUSES,
  type SchemaFieldDefault,
  type SchemaFieldInput,
  type SchemaTemplate,
  type SchemaVersion,
} from '../index.js';

describe('schema contracts', () => {
  it('publishes supported value types and lifecycle states', () => {
    expect(SCHEMA_VALUE_TYPES).toContain('array');
    expect(SCHEMA_VERSION_STATUSES).toEqual(['draft', 'published', 'superseded']);
    expect(FIELD_SENSITIVITY_LEVELS).toContain('high');
  });

  it('distinguishes no default from every literal JSON value', () => {
    const noDefault: SchemaFieldDefault = { kind: 'none' };
    const emptyStringDefault: SchemaFieldDefault = { kind: 'literal', value: '' };
    const nullDefault: SchemaFieldDefault = { kind: 'literal', value: null };

    expect(noDefault.kind).toBe('none');
    expect(emptyStringDefault.value).toBe('');
    expect(nullDefault.value).toBeNull();
  });

  it('binds templates to immutable schema versions', () => {
    expectTypeOf<SchemaTemplate>().toHaveProperty('current_schema_version_id');
    expectTypeOf<SchemaVersion['fields']>().toBeArray();
  });

  it('keeps generated field IDs out of client input', () => {
    expectTypeOf<SchemaFieldInput>().not.toHaveProperty('id');
    expectTypeOf<SchemaFieldInput>().toHaveProperty('extraction_hint');
  });
});
