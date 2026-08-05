<script setup lang="ts">
defineOptions({ name: 'DmErrorState' });

withDefaults(
  defineProps<{
    title: string;
    description: string;
    requestId?: string;
    retryLabel?: string;
  }>(),
  {
    requestId: '',
    retryLabel: '重试',
  },
);

const emit = defineEmits<{
  retry: [];
}>();
</script>

<template>
  <section class="dm-state dm-state--error" role="alert">
    <div v-if="$slots.icon" class="dm-state__icon" aria-hidden="true"><slot name="icon" /></div>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <code v-if="requestId">Request ID: {{ requestId }}</code>
    <button type="button" class="dm-state__action" @click="emit('retry')">{{ retryLabel }}</button>
  </section>
</template>
