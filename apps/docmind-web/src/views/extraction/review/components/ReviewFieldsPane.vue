<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';
import { DmButton, DmInput } from '@/ui';

import type { ReviewFieldModel } from '../model/review-workspace.js';

defineProps<{
  fields: readonly ReviewFieldModel[];
  activeFieldId: string;
}>();

const emit = defineEmits<{
  select: [fieldId: string];
  update: [fieldId: string, value: string];
  accept: [fieldId: string];
}>();
</script>

<template>
  <section
    class="z-10 flex w-[28%] min-w-0 flex-col border-r border-zinc-200 bg-white shadow-[0_0_15px_rgb(0_0_0/0.02)]"
  >
    <header
      class="flex items-center justify-between border-b border-zinc-200 bg-white px-4 py-2 text-[11px] font-semibold tracking-wider text-zinc-500 uppercase shadow-subtle"
    >
      <span class="flex items-center gap-1.5"
        ><AppIcon name="layout-list" class="size-3.5" />结构化提取结果</span
      >
      <span class="flex items-center gap-1 text-emerald-600"
        ><AppIcon name="check-circle-2" class="size-3" />已自动保存</span
      >
    </header>
    <div class="flex-1 space-y-1.5 overflow-y-auto p-4">
      <article
        v-for="field in fields"
        :key="field.id"
        :class="[
          'group relative cursor-text rounded border p-2.5 transition-all',
          field.risk
            ? 'mt-3 border-amber-200/80 bg-amber-50/30'
            : activeFieldId === field.id
              ? 'border-brand-200 bg-brand-50/50'
              : 'border-transparent hover:border-zinc-200 hover:bg-zinc-50/80',
        ]"
        @mouseenter="emit('select', field.id)"
        @focusin="emit('select', field.id)"
      >
        <div class="mb-0.5 flex items-center justify-between">
          <label
            :for="`review-field-${field.id}`"
            :class="[
              'flex items-center gap-1 text-[11px] font-medium',
              field.risk ? 'font-semibold text-amber-800' : 'text-zinc-500',
            ]"
          >
            <AppIcon v-if="field.risk" name="alert-triangle" class="size-3" />{{ field.label }}
          </label>
          <span
            :class="[
              'flex items-center gap-1 font-mono text-[10px]',
              field.risk ? 'text-amber-600' : 'text-zinc-400',
            ]"
          >
            <AppIcon :name="field.risk ? 'bot' : 'sparkles'" class="size-2.5" />
            {{ field.risk ? '需确认' : `${field.confidence}%` }}
          </span>
        </div>
        <DmInput
          :id="`review-field-${field.id}`"
          :model-value="field.value"
          appearance="unstyled"
          type="text"
          class="-ml-1.5 w-full rounded border border-transparent bg-transparent px-1.5 py-1 text-[13px] font-medium text-zinc-900 outline-none focus:border-brand-300 focus:bg-white focus:ring-2 focus:ring-brand-100/50"
          @update:model-value="emit('update', field.id, String($event))"
        />
        <div v-if="field.risk" class="mt-2 flex items-center gap-2">
          <DmButton variant="secondary" @click="emit('accept', field.id)">
            修正为 {{ field.suggestion }}
          </DmButton>
          <DmButton variant="ghost">保留原文</DmButton>
        </div>
      </article>
    </div>
    <footer class="border-t border-zinc-200 bg-zinc-50/50 p-3">
      <p class="text-[10px] text-zinc-400">当前字段证据</p>
      <p class="mt-1 border-l-2 border-brand-300 pl-2 text-[11px] leading-5 text-zinc-700">
        {{ fields.find((field) => field.id === activeFieldId)?.evidence }}
      </p>
    </footer>
  </section>
</template>
