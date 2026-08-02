<script setup lang="ts">
import { ref } from 'vue';

withDefaults(
  defineProps<{
    accept?: string;
    disabled?: boolean;
  }>(),
  {
    accept: '',
    disabled: false,
  },
);

const emit = defineEmits<{
  change: [event: Event];
}>();

const inputRef = ref<HTMLInputElement | null>(null);

const reset = (): void => {
  if (inputRef.value !== null) inputRef.value.value = '';
};

defineExpose({ reset });
</script>

<template>
  <input
    ref="inputRef"
    type="file"
    :accept="accept"
    :disabled="disabled"
    class="dm-file-input"
    @change="emit('change', $event)"
  />
</template>
