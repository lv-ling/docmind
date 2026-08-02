import { defineComponent, h, type PropType } from 'vue';

export const DM_BUTTON_VARIANTS = ['primary', 'secondary', 'ghost', 'danger'] as const;
export type DmButtonVariant = (typeof DM_BUTTON_VARIANTS)[number];

export const DM_BUTTON_SIZES = ['small', 'medium', 'large'] as const;
export type DmButtonSize = (typeof DM_BUTTON_SIZES)[number];

export const DmButton = defineComponent({
  name: 'DmButton',
  props: {
    variant: {
      type: String as PropType<DmButtonVariant>,
      default: 'primary',
      validator: (value: string) => DM_BUTTON_VARIANTS.includes(value as DmButtonVariant),
    },
    size: {
      type: String as PropType<DmButtonSize>,
      default: 'medium',
      validator: (value: string) => DM_BUTTON_SIZES.includes(value as DmButtonSize),
    },
    type: {
      type: String as PropType<'button' | 'submit' | 'reset'>,
      default: 'button',
    },
    disabled: Boolean,
    loading: Boolean,
    loadingLabel: {
      type: String,
      default: '处理中',
    },
  },
  emits: {
    click: (event: MouseEvent) => event.type.length > 0,
  },
  setup(props, { emit, slots }) {
    const onClick = (event: MouseEvent): void => {
      if (props.disabled || props.loading) {
        event.preventDefault();
        return;
      }
      emit('click', event);
    };

    return () =>
      h(
        'button',
        {
          class: ['dm-button', `dm-button--${props.variant}`, `dm-button--${props.size}`],
          type: props.type,
          disabled: props.disabled || props.loading,
          'aria-busy': props.loading ? 'true' : undefined,
          onClick,
        },
        [
          props.loading ? h('span', { class: 'dm-button__spinner', 'aria-hidden': 'true' }) : null,
          h(
            'span',
            {
              class: 'dm-button__label',
              'aria-hidden': props.loading ? 'true' : undefined,
            },
            slots.default?.(),
          ),
          props.loading
            ? h('span', { class: 'dm-sr-only', role: 'status' }, props.loadingLabel)
            : null,
        ],
      );
  },
});
