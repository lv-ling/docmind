import { readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const APP_ROOT = dirname(dirname(dirname(fileURLToPath(import.meta.url))));
const SOURCE_ROOTS = ['src/views', 'src/components', 'src/layouts'];
const NATIVE_TAG_PATTERN = /<(?:button|a|input|select|textarea|svg)\b/u;
const DIRECT_ICON_IMPORT_PATTERN = /from\s+['"]lucide-vue-next['"]/u;
const UI_PRIMITIVE_SEGMENT = join('src', 'components', 'ui');

const collectVueFiles = (directory: string): string[] =>
  readdirSync(directory).flatMap((entry) => {
    const path = join(directory, entry);
    return statSync(path).isDirectory()
      ? collectVueFiles(path)
      : path.endsWith('.vue')
        ? [path]
        : [];
  });

describe('DocMind design system enforcement', () => {
  const files = SOURCE_ROOTS.flatMap((root) => collectVueFiles(join(APP_ROOT, root)));

  it('keeps native interactive elements inside shared UI primitives', () => {
    const violations = files.flatMap((file) => {
      if (file.includes(UI_PRIMITIVE_SEGMENT)) return [];
      const source = readFileSync(file, 'utf8');
      return NATIVE_TAG_PATTERN.test(source) ? [relative(APP_ROOT, file)] : [];
    });

    expect(violations).toEqual([]);
  });

  it('routes Lucide usage through AppIcon', () => {
    const violations = files.flatMap((file) => {
      if (file.endsWith(join('src', 'components', 'AppIcon.vue'))) return [];
      const source = readFileSync(file, 'utf8');
      return DIRECT_ICON_IMPORT_PATTERN.test(source) ? [relative(APP_ROOT, file)] : [];
    });

    expect(violations).toEqual([]);
  });
});
