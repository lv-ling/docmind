<script setup lang="ts">
defineOptions({ name: 'DmEmptyState' });

withDefaults(
  defineProps<{
    title: string;
    description?: string;
    actionLabel?: string;
  }>(),
  {
    description: '',
    actionLabel: '',
  },
);

const emit = defineEmits<{
  action: [];
}>();
</script>

<template>
  <section class="dm-state dm-state--empty">
    <div v-if="$slots.icon" class="dm-state__icon" aria-hidden="true"><slot name="icon" /></div>
    <h2>{{ title }}</h2>
    <p v-if="description">{{ description }}</p>
    <button v-if="actionLabel" type="button" class="dm-state__action" @click="emit('action')">
      {{ actionLabel }}
    </button>
    <slot name="actions" />
  </section>
</template>
