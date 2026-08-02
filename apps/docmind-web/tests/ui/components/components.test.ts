import { renderToString } from '@vue/server-renderer';
import { createSSRApp, h, type Component, type VNodeChild } from 'vue';
import { describe, expect, it } from 'vitest';

import { DmButton, DmProgress, DmSplitPane, DmStatus, DmTabs, DmTextField } from '@/ui';

type TestSlots = Record<string, () => VNodeChild>;

const renderComponent = async (
  component: Component,
  props: Record<string, unknown>,
  slots?: TestSlots,
): Promise<string> =>
  renderToString(
    createSSRApp({
      render: () => h(component, props, slots),
    }),
  );

describe('UI component rendering contracts', () => {
  it('renders a loading button as disabled and exposes only the loading label', async () => {
    const html = await renderComponent(
      DmButton,
      { loading: true, loadingLabel: '正在保存' },
      { default: () => '保存模板' },
    );

    expect(html).toContain('disabled');
    expect(html).toContain('aria-busy="true"');
    expect(html).toContain('dm-button__label" aria-hidden="true"');
    expect(html).toContain('保存模板');
    expect(html).toContain('role="status">正在保存</span>');
  });

  it('connects a text field to its description and validation error', async () => {
    const html = await renderComponent(DmTextField, {
      id: 'party-name',
      label: '甲方名称',
      description: '应与营业执照一致',
      error: '甲方名称不能为空',
      required: true,
    });

    expect(html).toContain('for="party-name"');
    expect(html).toContain('id="party-name-description"');
    expect(html).toContain('id="party-name-error"');
    expect(html).toContain('aria-invalid="true"');
    expect(html).toContain('aria-describedby="party-name-description party-name-error"');
    expect(html).toContain('role="alert">甲方名称不能为空</p>');
  });

  it('announces a live status without making decorative marks readable', async () => {
    const html = await renderComponent(DmStatus, {
      label: '敏感信息扫描完成',
      tone: 'success',
      live: true,
    });

    expect(html).toContain('role="status"');
    expect(html).toContain('aria-live="polite"');
    expect(html).toContain('dm-status--success');
    expect(html).toContain('dm-status__dot" aria-hidden="true"');
  });

  it('clamps progress values and exposes the normalized percentage', async () => {
    const html = await renderComponent(DmProgress, {
      value: 130,
      max: 100,
      label: '实体抽取进度',
      showValue: true,
    });

    expect(html).toContain('role="progressbar"');
    expect(html).toContain('aria-label="实体抽取进度"');
    expect(html).toContain('aria-valuenow="100"');
    expect(html).toContain('aria-valuetext="100%"');
    expect(html).toContain('width:100%');
  });

  it('renders controlled tabs with selected and disabled states', async () => {
    const html = await renderComponent(DmTabs, {
      modelValue: 'all',
      label: '文档状态',
      items: [
        { value: 'all', label: '全部文档', count: 1204 },
        { value: 'review', label: '待审核', count: 3 },
        { value: 'archived', label: '已归档', disabled: true },
      ],
    });

    expect(html).toContain('role="tablist"');
    expect(html).toContain('aria-label="文档状态"');
    expect(html).toContain('aria-selected="true"');
    expect(html).toContain('dm-tabs__tab--active');
    expect(html).toContain('disabled');
  });

  it('exposes keyboard-operable split pane semantics and panel controls', async () => {
    const html = await renderComponent(
      DmSplitPane,
      { modelValue: 64, minimum: 25, maximum: 75 },
      { left: () => '原始合同', right: () => '可编辑模板' },
    );

    expect(html).toContain('role="separator"');
    expect(html).toContain('tabindex="0"');
    expect(html).toContain('aria-valuemin="25"');
    expect(html).toContain('aria-valuemax="75"');
    expect(html).toContain('aria-valuenow="64"');
    expect(html).toContain('aria-label="收起原件"');
    expect(html).toContain('aria-label="收起模板"');
    expect(html).toMatch(/aria-controls="v-[^"]+-left v-[^"]+-right"/u);
  });

  it('keeps one split pane visible if a controlled parent collapses both', async () => {
    const html = await renderComponent(
      DmSplitPane,
      { leftCollapsed: true, rightCollapsed: true },
      { left: () => '原始合同', right: () => '可编辑模板' },
    );

    expect(html).not.toContain('dm-split-pane--left-collapsed');
    expect(html).toContain('dm-split-pane--right-collapsed');
    expect(html).toContain('aria-label="原件"');
    expect(html).toContain('原始合同');
    expect(html).toContain('aria-label="模板" hidden');
    expect(html).toContain('可编辑模板');
  });
});
