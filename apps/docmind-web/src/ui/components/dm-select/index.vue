<script setup lang="ts">
import { computed } from 'vue';

export interface DmSelectOption {
  label: string;
  value: string;
  disabled?: boolean;
}

defineOptions({ name: 'DmSelect' });

const props = withDefaults(
  defineProps<{
    id: string;
    label: string;
    options: DmSelectOption[];
    placeholder?: string;
    description?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
  }>(),
  {
    placeholder: '',
    description: '',
    error: '',
    required: false,
    disabled: false,
  },
);

const modelValue = defineModel<string>({ default: '' });
const descriptionId = computed(() => `${props.id}-description`);
const errorId = computed(() => `${props.id}-error`);
const describedBy = computed(() =>
  [
    props.description.length > 0 ? descriptionId.value : null,
    props.error.length > 0 ? errorId.value : null,
  ]
    .filter((value): value is string => value !== null)
    .join(' '),
);
</script>

<template>
  <div :class="['dm-field', error.length > 0 ? 'dm-field--invalid' : null]">
    <label class="dm-field__label" :for="id">
      {{ label }}<span v-if="required" class="dm-field__required" aria-hidden="true"> *</span>
    </label>
    <p v-if="description" :id="descriptionId" class="dm-field__description">{{ description }}</p>
    <select
      :id="id"
      v-model="modelValue"
      class="dm-field__control dm-select__control"
      :required="required"
      :disabled="disabled"
      :aria-invalid="error.length > 0 || undefined"
      :aria-describedby="describedBy || undefined"
    >
      <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
      <option
        v-for="option in options"
        :key="option.value"
        :value="option.value"
        :disabled="option.disabled"
      >
        {{ option.label }}
      </option>
    </select>
    <p v-if="error" :id="errorId" class="dm-field__error" role="alert">{{ error }}</p>
  </div>
</template>
