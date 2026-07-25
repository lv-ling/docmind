import { readFile } from 'node:fs/promises';

import { describe, expect, it } from 'vitest';

interface PackageManifest {
  name: string;
  packageManager: string;
  private: boolean;
}

describe('workspace manifest', () => {
  it('keeps the workspace private and pins pnpm', async () => {
    const manifest = JSON.parse(
      await readFile(new URL('../../package.json', import.meta.url), 'utf8'),
    ) as PackageManifest;

    expect(manifest.name).toBe('docmind');
    expect(manifest.private).toBe(true);
    expect(manifest.packageManager).toBe('pnpm@10.13.1');
  });
});
