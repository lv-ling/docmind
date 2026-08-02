import { computed, defineComponent, h, onBeforeUnmount, ref, useId } from 'vue';

const clamp = (value: number, minimum: number, maximum: number): number =>
  Math.min(maximum, Math.max(minimum, value));

export const DmSplitPane = defineComponent({
  name: 'DmSplitPane',
  props: {
    modelValue: { type: Number, default: 50 },
    minimum: { type: Number, default: 20 },
    maximum: { type: Number, default: 80 },
    leftCollapsed: Boolean,
    rightCollapsed: Boolean,
    leftLabel: { type: String, default: '原件' },
    rightLabel: { type: String, default: '模板' },
  },
  emits: {
    'update:modelValue': (value: number) => Number.isFinite(value),
    'update:leftCollapsed': (value: boolean) => typeof value === 'boolean',
    'update:rightCollapsed': (value: boolean) => typeof value === 'boolean',
  },
  setup(props, { emit, slots }) {
    const componentId = useId();
    const leftPanelId = `${componentId}-left`;
    const rightPanelId = `${componentId}-right`;
    const container = ref<HTMLElement | null>(null);
    const percentage = computed(() => clamp(props.modelValue, props.minimum, props.maximum));
    // Fail safe when a controlled parent temporarily requests both panels collapsed.
    const leftIsCollapsed = computed(() => props.leftCollapsed && !props.rightCollapsed);
    const rightIsCollapsed = computed(() => props.rightCollapsed);

    const updateFromPointer = (event: PointerEvent): void => {
      const rect = container.value?.getBoundingClientRect();
      if (rect === undefined || rect.width === 0) return;
      emit(
        'update:modelValue',
        clamp(((event.clientX - rect.left) / rect.width) * 100, props.minimum, props.maximum),
      );
    };
    const endPointerDrag = (): void => {
      if (typeof window === 'undefined') return;
      window.removeEventListener('pointermove', updateFromPointer);
      window.removeEventListener('pointerup', endPointerDrag);
    };
    const startPointerDrag = (event: PointerEvent): void => {
      if (typeof window === 'undefined') return;
      event.preventDefault();
      window.addEventListener('pointermove', updateFromPointer);
      window.addEventListener('pointerup', endPointerDrag, { once: true });
    };
    const onSeparatorKeydown = (event: KeyboardEvent): void => {
      const step = event.shiftKey ? 10 : 2;
      let next = percentage.value;
      if (event.key === 'ArrowLeft') next -= step;
      else if (event.key === 'ArrowRight') next += step;
      else if (event.key === 'Home') next = props.minimum;
      else if (event.key === 'End') next = props.maximum;
      else return;
      event.preventDefault();
      emit('update:modelValue', clamp(next, props.minimum, props.maximum));
    };

    const toggleLeft = (): void => {
      if (!leftIsCollapsed.value && rightIsCollapsed.value) return;
      emit('update:leftCollapsed', !leftIsCollapsed.value);
    };
    const toggleRight = (): void => {
      if (!rightIsCollapsed.value && leftIsCollapsed.value) return;
      emit('update:rightCollapsed', !rightIsCollapsed.value);
    };

    onBeforeUnmount(endPointerDrag);

    return () =>
      h(
        'section',
        {
          ref: container,
          class: [
            'dm-split-pane',
            leftIsCollapsed.value ? 'dm-split-pane--left-collapsed' : null,
            rightIsCollapsed.value ? 'dm-split-pane--right-collapsed' : null,
          ],
          style: { '--dm-split-left': `${percentage.value}%` },
        },
        [
          h(
            'section',
            {
              id: leftPanelId,
              class: 'dm-split-pane__panel dm-split-pane__panel--left',
              'aria-label': props.leftLabel,
              hidden: leftIsCollapsed.value,
            },
            slots.left?.(),
          ),
          h('div', { class: 'dm-split-pane__divider' }, [
            h(
              'button',
              {
                type: 'button',
                class: 'dm-split-pane__collapse',
                'aria-controls': leftPanelId,
                'aria-expanded': leftIsCollapsed.value ? 'false' : 'true',
                'aria-label': leftIsCollapsed.value
                  ? `展开${props.leftLabel}`
                  : `收起${props.leftLabel}`,
                onClick: toggleLeft,
              },
              leftIsCollapsed.value ? '›' : '‹',
            ),
            h('div', {
              class: 'dm-split-pane__separator',
              role: 'separator',
              tabindex: 0,
              'aria-controls': `${leftPanelId} ${rightPanelId}`,
              'aria-label': `调整${props.leftLabel}与${props.rightLabel}宽度`,
              'aria-orientation': 'vertical',
              'aria-valuemin': props.minimum,
              'aria-valuemax': props.maximum,
              'aria-valuenow': Math.round(percentage.value),
              onPointerdown: startPointerDrag,
              onKeydown: onSeparatorKeydown,
            }),
            h(
              'button',
              {
                type: 'button',
                class: 'dm-split-pane__collapse',
                'aria-controls': rightPanelId,
                'aria-expanded': rightIsCollapsed.value ? 'false' : 'true',
                'aria-label': rightIsCollapsed.value
                  ? `展开${props.rightLabel}`
                  : `收起${props.rightLabel}`,
                onClick: toggleRight,
              },
              rightIsCollapsed.value ? '‹' : '›',
            ),
          ]),
          h(
            'section',
            {
              id: rightPanelId,
              class: 'dm-split-pane__panel dm-split-pane__panel--right',
              'aria-label': props.rightLabel,
              hidden: rightIsCollapsed.value,
            },
            slots.right?.(),
          ),
        ],
      );
  },
});
