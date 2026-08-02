import { describe, expect, expectTypeOf, it } from 'vitest';

import {
  SENSITIVE_DATA_TYPES,
  SENSITIVE_RECOGNIZER_KINDS,
  SENSITIVE_REGEX_DIALECTS,
  SENSITIVE_SUPPORTED_COUNTRY_CODES,
  SENSITIVE_TOKEN_PATTERN_SOURCE,
  SENSITIVE_VALIDATOR_NAMES,
  type SensitiveRuleDefinition,
  type SensitiveRuleInput,
  type SensitiveTokenReference,
} from '../index.js';

describe('sensitive-data contracts', () => {
  it('covers required built-in and custom sensitive types', () => {
    expect(SENSITIVE_DATA_TYPES).toEqual(
      expect.arrayContaining([
        'china_national_id',
        'identity_document',
        'phone_number',
        'email_address',
        'custom',
      ]),
    );
    expect(SENSITIVE_RECOGNIZER_KINDS).toEqual(['presidio', 'regex', 'dictionary', 'validator']);
    expect(SENSITIVE_REGEX_DIALECTS).toEqual(['re2']);
    expect(SENSITIVE_VALIDATOR_NAMES).toContain('cn_resident_identity');
    expect(SENSITIVE_SUPPORTED_COUNTRY_CODES).toEqual([
      'CN',
      'US',
      'JP',
      'KR',
      'DE',
      'FR',
      'GB',
      'AU',
      'NL',
    ]);
  });

  it('defines a strict stable-token wire format', () => {
    const pattern = new RegExp(SENSITIVE_TOKEN_PATTERN_SOURCE, 'u');

    expect(pattern.test('[[SENSITIVE:PHONE_NUMBER:01]]')).toBe(true);
    expect(pattern.test('plain@example.com')).toBe(false);
  });

  it('never exposes original or encrypted values in token references', () => {
    expectTypeOf<SensitiveTokenReference>().not.toHaveProperty('original_value');
    expectTypeOf<SensitiveTokenReference>().not.toHaveProperty('encrypted_value');
    expectTypeOf<SensitiveRuleDefinition['confidence_threshold']>().toBeNumber();
  });

  it('keeps generated rule IDs out of client input', () => {
    expectTypeOf<SensitiveRuleInput>().not.toHaveProperty('id');
  });
});
