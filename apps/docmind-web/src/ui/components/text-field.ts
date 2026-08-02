import { defineComponent, h, type PropType } from 'vue';

export const DM_TEXT_FIELD_TYPES = ['text', 'email', 'tel', 'password', 'number', 'date'] as const;
export type DmTextFieldType = (typeof DM_TEXT_FIELD_TYPES)[number];

export const DmTextField = defineComponent({
  name: 'DmTextField',
  props: {
    id: { type: String, required: true },
    modelValue: { type: [String, Number], default: '' },
    label: { type: String, required: true },
    type: {
      type: String as PropType<DmTextFieldType>,
      default: 'text',
      validator: (value: string) => DM_TEXT_FIELD_TYPES.includes(value as DmTextFieldType),
    },
    description: { type: String, default: '' },
    error: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    autocomplete: { type: String, default: 'off' },
    required: Boolean,
    disabled: Boolean,
    readonly: Boolean,
  },
  emits: {
    'update:modelValue': (value: string) => typeof value === 'string',
    blur: (event: FocusEvent) => event.type.length > 0,
  },
  setup(props, { emit }) {
    return () => {
      const descriptionId = `${props.id}-description`;
      const errorId = `${props.id}-error`;
      const describedBy = [
        props.description.length > 0 ? descriptionId : null,
        props.error.length > 0 ? errorId : null,
      ]
        .filter((value): value is string => value !== null)
        .join(' ');

      return h(
        'div',
        { class: ['dm-field', props.error.length > 0 ? 'dm-field--invalid' : null] },
        [
          h('label', { class: 'dm-field__label', for: props.id }, [
            props.label,
            props.required
              ? h('span', { class: 'dm-field__required', 'aria-hidden': 'true' }, ' *')
              : null,
          ]),
          props.description.length > 0
            ? h('p', { id: descriptionId, class: 'dm-field__description' }, props.description)
            : null,
          h('input', {
            id: props.id,
            class: 'dm-field__control',
            value: props.modelValue,
            type: props.type,
            placeholder: props.placeholder,
            autocomplete: props.autocomplete,
            required: props.required,
            disabled: props.disabled,
            readonly: props.readonly,
            'aria-invalid': props.error.length > 0 ? 'true' : undefined,
            'aria-describedby': describedBy.length > 0 ? describedBy : undefined,
            onInput: (event: Event) =>
              emit('update:modelValue', (event.target as HTMLInputElement).value),
            onBlur: (event: FocusEvent) => emit('blur', event),
          }),
          props.error.length > 0
            ? h('p', { id: errorId, class: 'dm-field__error', role: 'alert' }, props.error)
            : null,
        ],
      );
    };
  },
});
