// @vitest-environment happy-dom

import { renderToString } from '@vue/server-renderer';
import { mount } from '@vue/test-utils';
import { createSSRApp, h } from 'vue';
import { describe, expect, it } from 'vitest';

import {
  DocumentPicker,
  DocumentSourceBadge,
  DocumentStatusBadge,
  DocumentTaskProgress,
} from '@/components/index.js';

describe('document shared components', () => {
  it('selects an eligible document and ignores blocked options', async () => {
    const wrapper = mount(DocumentPicker, {
      props: {
        id: 'extraction-input',
        modelValue: null,
        label: '选择抽取输入',
        items: [
          {
            id: 'source-1',
            name: '采购合同',
            sourceType: 'source-version',
            status: 'ready',
          },
          {
            id: 'source-2',
            name: '校验失败文档',
            sourceType: 'source-version',
            status: 'validation_failed',
            disabled: true,
            disabledReason: '安全校验未通过',
          },
        ],
      },
    });

    const options = wrapper.findAll('[role="radio"]');
    await options[0]?.trigger('click');
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['source-1']);
    expect(options[1]?.attributes('disabled')).toBeDefined();
    expect(wrapper.text()).toContain('安全校验未通过');
  });

  it('maps document source and lifecycle status labels', async () => {
    const html = await renderToString(
      createSSRApp({
        render: () =>
          h('div', [
            h(DocumentSourceBadge, { type: 'business-document' }),
            h(DocumentStatusBadge, { status: 'preview_failed' }),
          ]),
      }),
    );

    expect(html).toContain('业务文档');
    expect(html).toContain('预览失败');
    expect(html).toContain('dm-status--warning');
  });

  it('derives the active task step from a stable step id', async () => {
    const html = await renderToString(
      createSSRApp({
        render: () =>
          h(DocumentTaskProgress, {
            currentStepId: 'review',
            steps: [
              { id: 'upload', label: '上传' },
              { id: 'review', label: '人工复核' },
              { id: 'done', label: '完成' },
            ],
          }),
      }),
    );

    expect(html).toContain('aria-current="step"');
    expect(html).toContain('人工复核');
  });
});
