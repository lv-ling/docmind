// @vitest-environment happy-dom

import { renderToString } from '@vue/server-renderer';
import { mount } from '@vue/test-utils';
import { createSSRApp, h, type Component, type VNodeChild } from 'vue';
import { describe, expect, it } from 'vitest';

import {
  DmButton,
  DmAlertDialog,
  DmDataTable,
  DmDialog,
  DmDrawer,
  DmErrorState,
  DmFilterBar,
  DmPagination,
  DmProgress,
  DmSelect,
  DmSplitPane,
  DmStatus,
  DmStepper,
  DmTabs,
  DmTextField,
  DmTimeline,
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

  it('renders a labelled native select with disabled options and errors', async () => {
    const html = await renderComponent(DmSelect, {
      id: 'document-status',
      label: '文档状态',
      modelValue: '',
      placeholder: '请选择状态',
      error: '请选择文档状态',
      options: [
        { label: '可用', value: 'ready' },
        { label: '处理中', value: 'processing', disabled: true },
      ],
    });

    expect(html).toContain('for="document-status"');
    expect(html).toContain('请选择状态');
    expect(html).toContain('value="processing" disabled');
    expect(html).toContain('aria-invalid="true"');
    expect(html).toContain('role="alert">请选择文档状态</p>');
  });

  it('connects tab controls to the active panel', async () => {
    const html = await renderComponent(
      DmTabs,
      {
        modelValue: 'all',
        ariaLabel: '模板筛选',
        items: [
          { label: '全部模板', value: 'all', count: 6 },
          { label: '我的草稿', value: 'draft' },
        ],
      },
      { default: () => '全部模板内容' },
    );

    expect(html).toContain('role="tablist"');
    expect(html).toContain('aria-label="模板筛选"');
    expect(html).toContain('aria-selected="true"');
    expect(html).toContain('role="tabpanel"');
    expect(html).toContain('全部模板内容');
  });

  it('renders determinate and indeterminate progress semantics', async () => {
    const determinateHtml = await renderComponent(DmProgress, {
      label: '安全校验',
      value: 64,
    });
    const indeterminateHtml = await renderComponent(DmProgress, {
      label: '正在分析',
    });

    expect(determinateHtml).toContain('aria-valuenow="64"');
    expect(determinateHtml).toContain('64%');
    expect(indeterminateHtml).not.toContain('aria-valuenow');
    expect(indeterminateHtml).toContain('dm-progress__value--indeterminate');
  });

  it('renders a recoverable error with a safe request id', async () => {
    const html = await renderComponent(DmErrorState, {
      title: '预览加载失败',
      description: '原件未受影响，可以重新加载预览。',
      requestId: 'req-demo-42',
    });

    expect(html).toContain('role="alert"');
    expect(html).toContain('原件未受影响');
    expect(html).toContain('Request ID: req-demo-42');
    expect(html).toContain('>重试</button>');
  });

  it('traps modal semantics in a teleported dialog and emits close', async () => {
    const wrapper = mount(DmDialog, {
      attachTo: document.body,
      props: {
        open: true,
        title: '上传文档',
        description: '支持 DOC、DOCX 和 PDF。',
      },
      slots: {
        default: '上传内容',
      },
    });

    const dialog = document.body.querySelector<HTMLElement>('[role="dialog"]');
    const closeButton = document.body.querySelector<HTMLButtonElement>('.dm-dialog__close');
    expect(dialog?.getAttribute('aria-modal')).toBe('true');
    expect(dialog?.textContent).toContain('上传内容');
    closeButton?.click();
    expect(wrapper.emitted('close')).toHaveLength(1);

    wrapper.unmount();
  });

  it('renders sortable tabular data and emits the next sort direction', async () => {
    const columns = [
      {
        key: 'name',
        header: '文档名称',
        accessor: (row: object) => (row as { name: string }).name,
        sortable: true,
      },
    ];
    const wrapper = mount(DmDataTable, {
      props: {
        caption: '知识库文档',
        columns,
        rows: [{ id: 'doc-1', name: '采购合同' }],
        rowKey: (row: object) => (row as { id: string }).id,
        sort: { key: 'name', direction: 'asc' },
      },
    });

    expect(wrapper.get('caption').text()).toBe('知识库文档');
    expect(wrapper.get('th').attributes('aria-sort')).toBe('ascending');
    expect(wrapper.text()).toContain('采购合同');
    await wrapper.get('.dm-data-table__sort').trigger('click');
    expect(wrapper.emitted('sort')?.[0]).toEqual([{ key: 'name', direction: 'desc' }]);
  });

  it('keeps pagination summary and active page semantics in sync', async () => {
    const wrapper = mount(DmPagination, {
      props: {
        modelValue: 2,
        totalPages: 10,
        totalItems: 95,
        pageSize: 10,
      },
    });

    expect(wrapper.text()).toContain('显示 11–20');
    expect(wrapper.get('[aria-current="page"]').text()).toBe('2');
    await wrapper.get('[aria-label="下一页"]').trigger('click');
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([3]);
  });

  it('groups filter controls, actions, and a live result summary', async () => {
    const html = await renderComponent(
      DmFilterBar,
      { ariaLabel: '筛选文档' },
      {
        default: () => '状态筛选',
        actions: () => '重置筛选',
        summary: () => '找到 12 份文档',
      },
    );

    expect(html).toContain('aria-label="筛选文档"');
    expect(html).toContain('状态筛选');
    expect(html).toContain('重置筛选');
    expect(html).toContain('aria-live="polite"');
    expect(html).toContain('找到 12 份文档');
  });

  it('marks the active workflow step and keeps timeline events semantic', async () => {
    const stepperHtml = await renderComponent(DmStepper, {
      currentIndex: 1,
      items: [{ label: '上传' }, { label: '安全校验' }, { label: '完成' }],
    });
    const timelineHtml = await renderComponent(DmTimeline, {
      items: [
        {
          id: 'event-1',
          title: '预览生成完成',
          metadata: '10:30',
          tone: 'success',
        },
      ],
    });

    expect(stepperHtml).toContain('aria-current="step"');
    expect(stepperHtml).toContain('安全校验');
    expect(timelineHtml).toContain('dm-timeline__item--success');
    expect(timelineHtml).toContain('<time>10:30</time>');
  });

  it('provides alert and drawer variants on the shared modal foundation', () => {
    const alertWrapper = mount(DmAlertDialog, {
      attachTo: document.body,
      props: {
        open: true,
        title: '删除版本',
        description: '该操作无法撤销。',
      },
    });

    expect(document.body.querySelector('[role="alertdialog"]')).not.toBeNull();
    alertWrapper.unmount();

    const drawerWrapper = mount(DmDrawer, {
      attachTo: document.body,
      props: {
        open: true,
        title: '任务详情',
      },
    });

    expect(document.body.querySelector('.dm-dialog__panel--right')).not.toBeNull();
    drawerWrapper.unmount();
  });
});
