import { defineComponent, h, type PropType } from 'vue';

export const DM_STATUS_TONES = ['neutral', 'info', 'success', 'warning', 'danger'] as const;
export type DmStatusTone = (typeof DM_STATUS_TONES)[number];

export const DmStatus = defineComponent({
  name: 'DmStatus',
  props: {
    tone: {
      type: String as PropType<DmStatusTone>,
      default: 'neutral',
      validator: (value: string) => DM_STATUS_TONES.includes(value as DmStatusTone),
    },
    label: { type: String, required: true },
    live: Boolean,
  },
  setup(props) {
    return () =>
      h(
        'span',
        {
          class: ['dm-status', `dm-status--${props.tone}`],
          role: props.live ? 'status' : undefined,
          'aria-live': props.live ? 'polite' : undefined,
        },
        [
          h('span', { class: 'dm-status__dot', 'aria-hidden': 'true' }),
          h('span', { class: 'dm-status__label' }, props.label),
        ],
      );
  },
});
