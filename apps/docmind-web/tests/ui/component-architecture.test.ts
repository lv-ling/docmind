import { readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

const APP_ROOT = dirname(dirname(dirname(fileURLToPath(import.meta.url))));
const BUSINESS_ROOTS = ['src/views', 'src/layouts', 'src/components'];
const INTERNAL_COMPONENT_SEGMENTS = [
  join('src', 'components', 'ui'),
  join('src', 'components', 'dm'),
];
const SHADCN_IMPORT_PATTERN = /from\s+['"](?:@\/components\/ui(?:\/[^'"]*)?|reka-ui)['"]/u;

const collectSourceFiles = (directory: string): string[] =>
  readdirSync(directory).flatMap((entry) => {
    const path = join(directory, entry);
    return statSync(path).isDirectory()
      ? collectSourceFiles(path)
      : /\.(?:ts|vue)$/u.test(path)
        ? [path]
        : [];
  });

describe('DocMind component architecture boundaries', () => {
  it('keeps business pages and layouts independent from shadcn primitives', () => {
    const violations = BUSINESS_ROOTS.flatMap((root) => collectSourceFiles(join(APP_ROOT, root)))
      .filter((file) => !INTERNAL_COMPONENT_SEGMENTS.some((segment) => file.includes(segment)))
      .flatMap((file) =>
        SHADCN_IMPORT_PATTERN.test(readFileSync(file, 'utf8')) ? [relative(APP_ROOT, file)] : [],
      );

    expect(violations).toEqual([]);
  });

  it.each([
    ['DmButton.vue', '@/components/ui/button'],
    ['DmInput.vue', '@/components/ui/input'],
    ['DmTextarea.vue', '@/components/ui/textarea'],
    ['DmCheckbox.vue', '@/components/ui/checkbox'],
    ['DmSelect.vue', '@/components/ui/native-select'],
    ['DmTabs.vue', '@/components/ui/tabs'],
    ['DmDialog.vue', '@/components/ui/dialog'],
    ['DmDropdown.vue', '@/components/ui/dropdown-menu'],
    ['DmPopover.vue', '@/components/ui/popover'],
  ])('routes %s through its shadcn primitive', (file, primitiveImport) => {
    const source = readFileSync(join(APP_ROOT, 'src/components/dm', file), 'utf8');

    expect(source).toContain(`from '${primitiveImport}'`);
  });
});
