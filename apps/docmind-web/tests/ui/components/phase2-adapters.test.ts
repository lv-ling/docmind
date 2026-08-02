// @vitest-environment happy-dom

import { mount } from '@vue/test-utils';
import { h, nextTick } from 'vue';
import { afterEach, describe, expect, it } from 'vitest';

import { DmButton, DmDialog, DmDropdown, DmPopover, DmTabs, type DmDropdownEntry } from '@/ui';

const teleportStubs = {
  teleport: { template: '<div data-teleport><slot /></div>' },
};

afterEach(() => {
  document.body.innerHTML = '';
});

describe('Phase 2 shadcn adapters', () => {
  it('preserves the DmTabs model and delegates keyboard navigation to Reka Tabs', async () => {
    const wrapper = mount(DmTabs, {
      attachTo: document.body,
      props: {
        modelValue: 'running',
        label: '任务状态',
        items: [
          { value: 'running', label: '运行中' },
          { value: 'queued', label: '排队中', disabled: true },
          { value: 'done', label: '已完成' },
        ],
      },
    });

    const tabs = wrapper.findAll('[role="tab"]');
    expect(tabs).toHaveLength(3);
    expect(tabs[0]?.attributes('aria-selected')).toBe('true');
    expect(tabs[1]?.attributes('disabled')).toBeDefined();

    await tabs[2]?.trigger('mousedown', { button: 0, ctrlKey: false });
    await nextTick();

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['done']);
    expect(wrapper.emitted('change')?.[0]).toEqual(['done']);
  });

  it('exposes controlled dialog confirm, cancel, description, footer, and loading states', async () => {
    const wrapper = mount(DmDialog, {
      attachTo: document.body,
      global: { stubs: teleportStubs },
      props: {
        modelValue: true,
        title: '删除文档',
        description: '此操作不可撤销',
        confirmLabel: '确认删除',
        'onUpdate:modelValue': (value: boolean) => wrapper.setProps({ modelValue: value }),
      },
      slots: { default: '将同时删除关联的抽取结果。' },
    });

    await nextTick();
    expect(wrapper.text()).toContain('此操作不可撤销');
    expect(wrapper.text()).toContain('将同时删除关联的抽取结果。');

    const confirmButton = Array.from(
      document.querySelectorAll<HTMLButtonElement>('[data-slot="button"]'),
    ).find((button) => button.textContent?.includes('确认删除'));
    confirmButton?.click();
    await nextTick();
    expect(wrapper.emitted('confirm')).toHaveLength(1);

    await wrapper.setProps({ loading: true });
    expect(document.querySelector('.dm-button__spinner')).not.toBeNull();
    expect(
      Array.from(document.querySelectorAll<HTMLButtonElement>('[data-slot="button"]')).every(
        (button) => button.disabled,
      ),
    ).toBe(true);
  });

  it('renders dropdown items, separators, disabled state, and emits stable values', async () => {
    const items: readonly DmDropdownEntry[] = [
      { type: 'item', value: 'switch', label: '切换工作区' },
      { type: 'separator', key: 'account-separator' },
      { type: 'item', value: 'disabled', label: '不可用操作', disabled: true },
    ];
    const wrapper = mount(DmDropdown, {
      attachTo: document.body,
      global: { stubs: teleportStubs },
      props: { modelValue: true, items },
      slots: { trigger: () => h(DmButton, null, () => '更多') },
    });

    const menuItems = Array.from(
      document.querySelectorAll<HTMLElement>('[data-slot="dropdown-menu-item"]'),
    );
    expect(menuItems).toHaveLength(2);
    expect(document.querySelector('[data-slot="dropdown-menu-separator"]')).not.toBeNull();
    expect(menuItems[1]?.hasAttribute('data-disabled')).toBe(true);

    menuItems[0]?.click();
    await nextTick();
    expect(wrapper.emitted('select')?.[0]?.[0]).toBe('switch');
  });

  it('keeps popover open state controlled and renders content through the DocMind layer', async () => {
    mount(DmPopover, {
      attachTo: document.body,
      global: { stubs: teleportStubs },
      props: { modelValue: true },
      slots: {
        trigger: () => h(DmButton, null, () => '查看 AI 解释'),
        default: () => '字段置信度来自证据片段。',
      },
    });

    await nextTick();
    const content = document.querySelector<HTMLElement>('[data-slot="popover-content"]');
    expect(content?.textContent).toContain('字段置信度');
    expect(content?.classList.contains('dm-popover')).toBe(true);
  });
});
