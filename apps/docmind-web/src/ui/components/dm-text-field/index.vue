<script setup lang="ts">
import { computed } from 'vue';

type DmTextFieldType = 'text' | 'email' | 'tel' | 'password' | 'number' | 'date';

defineOptions({ name: 'DmTextField' });

const props = withDefaults(
  defineProps<{
    id: string;
    label: string;
    type?: DmTextFieldType;
    description?: string;
    error?: string;
    placeholder?: string;
    autocomplete?: string;
    required?: boolean;
    disabled?: boolean;
    readonly?: boolean;
  }>(),
  {
    type: 'text',
    description: '',
    error: '',
    placeholder: '',
    autocomplete: 'off',
    required: false,
    disabled: false,
    readonly: false,
  },
);

const modelValue = defineModel<string | number>({ default: '' });
const emit = defineEmits<{
  blur: [event: FocusEvent];
}>();

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

const handleInput = (event: Event): void => {
  modelValue.value = (event.target as HTMLInputElement).value;
};
</script>

<template>
  <div :class="['dm-field', error.length > 0 ? 'dm-field--invalid' : null]">
    <label class="dm-field__label" :for="id">
      {{ label }}<span v-if="required" class="dm-field__required" aria-hidden="true"> *</span>
    </label>
    <p v-if="description" :id="descriptionId" class="dm-field__description">{{ description }}</p>
    <input
      :id="id"
      class="dm-field__control"
      :value="modelValue"
      :type="type"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :required="required"
      :disabled="disabled"
      :readonly="readonly"
      :aria-invalid="error.length > 0 || undefined"
      :aria-describedby="describedBy || undefined"
      @input="handleInput"
      @blur="emit('blur', $event)"
    />
    <p v-if="error" :id="errorId" class="dm-field__error" role="alert">{{ error }}</p>
  </div>
</template>
