import { renderToString } from '@vue/server-renderer';
import { createSSRApp, h, type Component, type VNodeChild } from 'vue';
import { describe, expect, it } from 'vitest';

import {
  DmButton,
  DmCheckbox,
  DmInput,
  DmInteractiveSurface,
  DmProgress,
  DmRange,
  DmSelect,
  DmSplitPane,
  DmStatus,
  DmTabs,
  DmTextField,
  DmTextarea,
} from '@/ui';

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
    expect(html).toContain('data-slot="button"');
    expect(html).toContain('aria-busy="true"');
    expect(html).toContain('dm-button__label');
    expect(html).toContain('aria-hidden="true"');
    expect(html).toContain('保存模板');
    expect(html).toContain('role="status">正在保存</span>');
  });

  it('keeps icon-only and dark buttons inside the shared 30 px contract', async () => {
    const html = await renderComponent(
      DmButton,
      { variant: 'dark', iconOnly: true, 'aria-label': '返回' },
      { default: () => '图标' },
    );

    expect(html).toContain('dm-button--dark');
    expect(html).toContain('h-[30px]');
    expect(html).toContain('w-[30px]');
    expect(html).toContain('px-0');
    expect(html).toContain('aria-label="返回"');
  });

  it('renders shared select and checkbox controls with accessible labels', async () => {
    const selectHtml = await renderComponent(
      DmSelect,
      { id: 'file-type', modelValue: 'PDF', 'aria-label': '文件类型' },
      { default: () => h('option', { value: 'PDF' }, 'PDF') },
    );
    const checkboxHtml = await renderComponent(DmCheckbox, {
      id: 'required-field',
      label: '必填',
      description: '发布后应用到新任务',
      modelValue: true,
    });

    expect(selectHtml).toContain('id="file-type"');
    expect(selectHtml).toContain('data-slot="native-select"');
    expect(selectHtml).toContain('aria-label="文件类型"');
    expect(selectHtml).toContain('h-8');
    expect(checkboxHtml).toContain('for="required-field"');
    expect(checkboxHtml).toContain('data-slot="checkbox"');
    expect(checkboxHtml).toContain('id="required-field"');
    expect(checkboxHtml).toContain('checked');
    expect(checkboxHtml).toContain('发布后应用到新任务');
  });

  it('keeps shared input primitives on the same compact control baseline', async () => {
    const inputHtml = await renderComponent(DmInput, {
      id: 'query',
      modelValue: '合同',
      type: 'search',
    });
    const textareaHtml = await renderComponent(DmTextarea, {
      id: 'description',
      modelValue: '字段说明',
    });
    const rangeHtml = await renderComponent(DmRange, {
      id: 'confidence',
      modelValue: 82,
      min: 0,
      max: 100,
    });

    expect(inputHtml).toContain('id="query"');
    expect(inputHtml).toContain('data-slot="input"');
    expect(inputHtml).toContain('h-8');
    expect(textareaHtml).toContain('id="description"');
    expect(textareaHtml).toContain('data-slot="textarea"');
    expect(textareaHtml).toContain('rounded-compact');
    expect(rangeHtml).toContain('id="confidence"');
    expect(rangeHtml).toContain('accent-brand-600');
  });

  it('uses the shared interactive surface for composite list rows', async () => {
    const html = await renderComponent(
      DmInteractiveSurface,
      { 'aria-pressed': true },
      { default: () => '合同对象' },
    );

    expect(html).toContain('class="dm-interactive-surface"');
    expect(html).toContain('type="button"');
    expect(html).toContain('aria-pressed="true"');
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
