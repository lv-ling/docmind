<script setup lang="ts">
import { computed, onBeforeUnmount, ref, useId } from 'vue';

const props = withDefaults(
  defineProps<{
    modelValue?: number;
    minimum?: number;
    maximum?: number;
    leftCollapsed?: boolean;
    rightCollapsed?: boolean;
    leftLabel?: string;
    rightLabel?: string;
  }>(),
  {
    modelValue: 50,
    minimum: 20,
    maximum: 80,
    leftCollapsed: false,
    rightCollapsed: false,
    leftLabel: '原件',
    rightLabel: '模板',
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: number];
  'update:leftCollapsed': [value: boolean];
  'update:rightCollapsed': [value: boolean];
}>();

const clamp = (value: number): number => Math.min(props.maximum, Math.max(props.minimum, value));
const componentId = useId();
const leftPanelId = `${componentId}-left`;
const rightPanelId = `${componentId}-right`;
const containerRef = ref<HTMLElement | null>(null);
const percentage = computed(() => clamp(props.modelValue));
const isLeftCollapsed = computed(() => props.leftCollapsed && !props.rightCollapsed);
const isRightCollapsed = computed(() => props.rightCollapsed);

const updateFromPointer = (event: PointerEvent): void => {
  const rect = containerRef.value?.getBoundingClientRect();
  if (rect === undefined || rect.width === 0) return;
  emit('update:modelValue', clamp(((event.clientX - rect.left) / rect.width) * 100));
};

const stopPointerDrag = (): void => {
  window.removeEventListener('pointermove', updateFromPointer);
  window.removeEventListener('pointerup', stopPointerDrag);
};

const handlePointerDrag = (event: PointerEvent): void => {
  event.preventDefault();
  window.addEventListener('pointermove', updateFromPointer);
  window.addEventListener('pointerup', stopPointerDrag, { once: true });
};

const handleSeparatorKeydown = (event: KeyboardEvent): void => {
  const step = event.shiftKey ? 10 : 2;
  let next = percentage.value;
  if (event.key === 'ArrowLeft') next -= step;
  else if (event.key === 'ArrowRight') next += step;
  else if (event.key === 'Home') next = props.minimum;
  else if (event.key === 'End') next = props.maximum;
  else return;
  event.preventDefault();
  emit('update:modelValue', clamp(next));
};

const toggleLeft = (): void => {
  if (!isLeftCollapsed.value && isRightCollapsed.value) return;
  emit('update:leftCollapsed', !isLeftCollapsed.value);
};

const toggleRight = (): void => {
  if (!isRightCollapsed.value && isLeftCollapsed.value) return;
  emit('update:rightCollapsed', !isRightCollapsed.value);
};

onBeforeUnmount(stopPointerDrag);
</script>

<template>
  <section
    ref="containerRef"
    :class="[
      'dm-split-pane',
      isLeftCollapsed ? 'dm-split-pane--left-collapsed' : null,
      isRightCollapsed ? 'dm-split-pane--right-collapsed' : null,
    ]"
    :style="{ '--dm-split-left': `${percentage}%` }"
  >
    <section
      :id="leftPanelId"
      class="dm-split-pane__panel dm-split-pane__panel--left"
      :aria-label="leftLabel"
      :hidden="isLeftCollapsed"
    >
      <slot name="left" />
    </section>
    <div class="dm-split-pane__divider">
      <button
        type="button"
        class="dm-split-pane__collapse"
        :aria-controls="leftPanelId"
        :aria-expanded="!isLeftCollapsed"
        :aria-label="isLeftCollapsed ? `展开${leftLabel}` : `收起${leftLabel}`"
        @click="toggleLeft"
      >
        {{ isLeftCollapsed ? '›' : '‹' }}
      </button>
      <div
        class="dm-split-pane__separator"
        role="separator"
        tabindex="0"
        :aria-controls="`${leftPanelId} ${rightPanelId}`"
        :aria-label="`调整${leftLabel}与${rightLabel}宽度`"
        aria-orientation="vertical"
        :aria-valuemin="minimum"
        :aria-valuemax="maximum"
        :aria-valuenow="Math.round(percentage)"
        @pointerdown="handlePointerDrag"
        @keydown="handleSeparatorKeydown"
      ></div>
      <button
        type="button"
        class="dm-split-pane__collapse"
        :aria-controls="rightPanelId"
        :aria-expanded="!isRightCollapsed"
        :aria-label="isRightCollapsed ? `展开${rightLabel}` : `收起${rightLabel}`"
        @click="toggleRight"
      >
        {{ isRightCollapsed ? '‹' : '›' }}
      </button>
    </div>
    <section
      :id="rightPanelId"
      class="dm-split-pane__panel dm-split-pane__panel--right"
      :aria-label="rightLabel"
      :hidden="isRightCollapsed"
    >
      <slot name="right" />
    </section>
  </section>
</template>
