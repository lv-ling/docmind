import { readFileSync } from 'node:fs';

import { describe, expect, it } from 'vitest';

describe('OpenAPI artifact', () => {
  const document = readFileSync(new URL('../openapi/v1.yaml', import.meta.url), 'utf8');

  it('uses OpenAPI 3.1, authentication, and unique operation IDs', () => {
    expect(document).toContain('openapi: 3.1.0');
    expect(document).toContain('bearerAuth:');

    const operationIds = [...document.matchAll(/^\s+operationId:\s+(\S+)$/gmu)].map(
      (match) => match[1],
    );

    expect(operationIds.length).toBeGreaterThanOrEqual(20);
    expect(new Set(operationIds).size).toBe(operationIds.length);
  });

  it('defines every referenced local component', () => {
    const references = [
      ...document.matchAll(/#\/components\/(schemas|parameters|responses)\/([A-Za-z0-9]+)/gu),
    ];

    for (const reference of references) {
      expect(document).toMatch(new RegExp(`^ {4}${reference[2]}:`, 'mu'));
    }
  });
});
