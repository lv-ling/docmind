// @vitest-environment happy-dom

import { describe, expect, it } from 'vitest';

import { formatBytes, sha256Hex, validateSourceFile } from '@/utils/file.js';

describe('source file safeguards', () => {
  it('accepts supported extensions and does not trust the browser MIME value', () => {
    const file = new File(['document'], 'contract.DOCX', { type: 'text/plain' });

    expect(validateSourceFile(file)).toEqual({
      fileType: 'docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    });
  });

  it('rejects unsupported and empty files', () => {
    expect(() => validateSourceFile(new File(['x'], 'notes.txt'))).toThrow('仅支持');
    expect(() => validateSourceFile(new File([], 'empty.pdf'))).toThrow('空文件');
  });

  it('computes a stable SHA-256 and formats file sizes', async () => {
    const file = new File(['abc'], 'small.pdf', { type: 'application/pdf' });

    expect(await sha256Hex(file)).toBe(
      'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
    );
    expect(formatBytes(2 * 1024 * 1024)).toBe('2.0 MB');
  });
});
