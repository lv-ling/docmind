import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

describe('JSON Schema artifacts', () => {
  const schemaDirectory = fileURLToPath(new URL('../json-schema', import.meta.url));
  const schemaFiles = readdirSync(schemaDirectory).filter((file) => file.endsWith('.schema.json'));

  it('publishes parseable Draft 2020-12 schemas with stable identifiers', () => {
    expect(schemaFiles.length).toBeGreaterThanOrEqual(3);

    for (const file of schemaFiles) {
      const parsed = JSON.parse(readFileSync(resolve(schemaDirectory, file), 'utf8')) as Record<
        string,
        unknown
      >;

      expect(parsed.$schema).toBe('https://json-schema.org/draft/2020-12/schema');
      expect(parsed.$id).toBe(`https://docmind.local/schemas/${file}`);
      expect(parsed.title).toEqual(expect.any(String));
    }
  });
});
