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

  it('keeps shared editor and ignore rules in place', async () => {
    const [editorConfig, gitignore, dockerignore] = await Promise.all([
      readFile(new URL('../../.editorconfig', import.meta.url), 'utf8'),
      readFile(new URL('../../.gitignore', import.meta.url), 'utf8'),
      readFile(new URL('../../.dockerignore', import.meta.url), 'utf8'),
    ]);

    expect(editorConfig).toContain('root = true');
    expect(editorConfig).toContain('end_of_line = lf');
    expect(gitignore).toContain('.pnpm-store/');
    expect(gitignore).toContain('!**/.env.example');
    expect(dockerignore).toContain('**/node_modules');
    expect(dockerignore).toContain('**/.env');
  });
});
