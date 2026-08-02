import { describe, expect, it } from 'vitest';

import {
  createDefaultSensitiveRules,
  createEditableSchemaField,
  parseSchemaLiteral,
  toSchemaFieldInput,
} from '@/views/schema/list/model/schema-form.js';

describe('schema form model', () => {
  it('creates a useful first field and blank subsequent fields', () => {
    expect(createEditableSchemaField(0)).toMatchObject({
      key: 'document_title',
      valueType: 'string',
      nullable: true,
    });
    expect(createEditableSchemaField(1).key).toBe('');
  });

  it('parses supported default literal types', () => {
    expect(parseSchemaLiteral('12.5', 'number')).toBe(12.5);
    expect(parseSchemaLiteral('TRUE', 'boolean')).toBe(true);
    expect(parseSchemaLiteral('{"enabled":true}', 'object')).toEqual({ enabled: true });
    expect(() => parseSchemaLiteral('not-a-number', 'integer')).toThrow(
      '数字字段的默认值必须是有效数字',
    );
  });

  it('maps the page model to the server contract at one boundary', () => {
    const field = createEditableSchemaField(1);
    field.key = 'contract_total';
    field.description = '合同总额';
    field.valueType = 'number';
    field.sensitivity = 'high';
    field.defaultEnabled = true;
    field.defaultValue = '0';

    expect(toSchemaFieldInput(field, 2)).toMatchObject({
      key: 'contract_total',
      json_path: '$.contract_total',
      value_type: 'number',
      default: { kind: 'literal', value: 0 },
      sensitivity: 'high',
      display: { mask: 'full' },
      position: 2,
    });
  });

  it('creates the stable supported-country sensitive rule preset', () => {
    const rules = createDefaultSensitiveRules();
    expect(rules).toHaveLength(6);
    expect(rules.map((rule) => rule.priority)).toEqual([100, 90, 80, 70, 60, 50]);
    expect(rules.every((rule) => rule.country_codes.length === 9)).toBe(true);
  });
});
