// @vitest-environment happy-dom

import { mount } from '@vue/test-utils';
import { h } from 'vue';
import { describe, expect, it, vi } from 'vitest';

import AppIcon from '@/components/AppIcon.vue';
import { DmButton, DmCheckbox, DmInput, DmSelect, DmTextarea } from '@/ui';

describe('Phase 1 shadcn adapters', () => {
  it('keeps the DmButton loading guard at the wrapper boundary', async () => {
    const onClick = vi.fn();
    const wrapper = mount(DmButton, {
      props: { loading: true, onClick },
      slots: { default: 'Save' },
    });

    await wrapper.get('[data-slot="button"]').trigger('click');

    expect(onClick).not.toHaveBeenCalled();
    expect(wrapper.get('[data-slot="button"]').attributes('disabled')).toBeDefined();
    expect(wrapper.get('.dm-button__spinner').classes()).toEqual(
      expect.arrayContaining(['flex', 'size-3.5', 'shrink-0', 'items-center', 'justify-center']),
    );
  });

  it('keeps button icons and text on one centered flex baseline', () => {
    const wrapper = mount(DmButton, {
      slots: {
        default: () => [h(AppIcon, { name: 'plus' }), h('span', 'Create')],
      },
    });

    const button = wrapper.get('[data-slot="button"]');
    const iconWrapper = wrapper.get('.app-icon');
    const iconSvg = iconWrapper.get('svg');

    expect(button.classes()).toEqual(
      expect.arrayContaining([
        'inline-flex',
        'items-center',
        'justify-center',
        'gap-1.5',
        'leading-none',
      ]),
    );
    expect(iconWrapper.element.tagName).toBe('SPAN');
    expect(iconWrapper.classes()).toEqual(
      expect.arrayContaining([
        'inline-flex',
        'shrink-0',
        'items-center',
        'justify-center',
        'leading-none',
      ]),
    );
    expect(iconSvg.classes()).toEqual(
      expect.arrayContaining(['app-icon__svg', 'block', 'size-full']),
    );
    expect(iconSvg.attributes('stroke-width')).toBe('1.8');
  });

  it('forwards input and textarea models without exposing primitive APIs', async () => {
    const onInputUpdate = vi.fn();
    const onTextareaUpdate = vi.fn();
    const input = mount(DmInput, {
      props: { modelValue: 'draft', 'onUpdate:modelValue': onInputUpdate },
    });
    const textarea = mount(DmTextarea, {
      props: { modelValue: 'notes', 'onUpdate:modelValue': onTextareaUpdate },
    });

    await input.get('input').setValue('final');
    await textarea.get('textarea').setValue('reviewed');

    expect(onInputUpdate).toHaveBeenLastCalledWith('final');
    expect(onTextareaUpdate).toHaveBeenLastCalledWith('reviewed');
  });

  it('preserves native select options and the string model contract', async () => {
    const onUpdate = vi.fn();
    const wrapper = mount(DmSelect, {
      props: {
        id: 'document-kind',
        modelValue: 'pdf',
        'onUpdate:modelValue': onUpdate,
      },
      slots: {
        default: '<option value="pdf">PDF</option><option value="docx">DOCX</option>',
      },
    });

    await wrapper.get('select').setValue('docx');

    expect(onUpdate).toHaveBeenLastCalledWith('docx');
  });

  it('adapts the Reka checkbox state back to the boolean Dm model', async () => {
    const onUpdate = vi.fn();
    const wrapper = mount(DmCheckbox, {
      props: {
        id: 'is-required',
        label: 'Required',
        modelValue: false,
        'onUpdate:modelValue': onUpdate,
      },
    });

    await wrapper.get('[data-slot="checkbox"]').trigger('click');

    expect(onUpdate).toHaveBeenLastCalledWith(true);
  });
});
