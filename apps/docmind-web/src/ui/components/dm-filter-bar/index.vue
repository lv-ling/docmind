<script setup lang="ts">
defineOptions({ name: 'DmFilterBar' });

defineProps<{
  ariaLabel: string;
}>();
</script>

<template>
  <section class="dm-filter-bar" :aria-label="ariaLabel">
    <div class="dm-filter-bar__fields"><slot /></div>
    <div v-if="$slots.actions" class="dm-filter-bar__actions"><slot name="actions" /></div>
    <div v-if="$slots.summary" class="dm-filter-bar__summary" aria-live="polite">
      <slot name="summary" />
    </div>
  </section>
</template>
