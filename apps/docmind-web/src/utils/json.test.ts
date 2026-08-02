import { reactive } from 'vue';
import { describe, expect, it } from 'vitest';

import { cloneJsonValue } from './json.js';

describe('cloneJsonValue', () => {
  it('clones Vue reactive JSON values without retaining proxy state', () => {
    const source = reactive({ page: { title: '初始标题' }, blocks: ['正文'] });

    const clone = cloneJsonValue(source);
    source.page.title = '已修改';
    source.blocks.push('附录');

    expect(clone).toEqual({ page: { title: '初始标题' }, blocks: ['正文'] });
  });

  it('rejects values that JSON cannot represent as a root value', () => {
    expect(() => cloneJsonValue(undefined)).toThrow('Value is not JSON serializable');
  });
});
