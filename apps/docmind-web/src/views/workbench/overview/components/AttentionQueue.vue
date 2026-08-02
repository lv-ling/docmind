<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';

import type { WorkbenchAttentionItem } from '../model/workbench-overview.js';

defineProps<{
  items: WorkbenchAttentionItem[];
  attentionCount: number;
}>();

const emit = defineEmits<{
  'open-item': [item: WorkbenchAttentionItem];
}>();
</script>

<template>
  <section
    id="workbench-attention"
    class="attention-queue grid scroll-mt-18 gap-3"
    aria-labelledby="attention-title"
  >
    <header class="flex items-center gap-2">
      <h2 id="attention-title" class="m-0 text-[13px] leading-[19.5px] font-semibold text-zinc-800">
        需人工干预
      </h2>
      <span
        class="rounded-sm bg-[#fef3c7]/80 px-1.5 py-0.5 text-[10px] leading-[15px] font-semibold text-[#b45309]"
      >
        {{ attentionCount }} 份
      </span>
    </header>

    <div class="attention-queue__list grid gap-3">
      <article
        v-for="item in items"
        :key="item.id"
        class="attention-card group relative min-w-0 overflow-hidden rounded-md border border-zinc-200 bg-white shadow-subtle transition-[border-color,transform,box-shadow] duration-interaction ease-dm hover:-translate-y-px hover:border-brand-200 hover:shadow-[0_5px_16px_rgb(99_102_241_/_7%)] motion-reduce:transition-none"
        :class="{
          'attention-card--review flex flex-col p-4': item.variant === 'review',
          'attention-card--archive p-3': item.variant === 'archive',
        }"
      >
        <span
          v-if="item.variant === 'review'"
          class="attention-card__marker absolute inset-y-0 left-0 w-[3px] bg-[#fbbf24]"
          aria-hidden="true"
        ></span>

        <div
          class="attention-card__heading grid grid-cols-[28px_minmax(0,1fr)_auto] items-start gap-3 max-[700px]:grid-cols-[28px_minmax(0,1fr)]"
          :class="{ 'mb-3': item.variant === 'review' }"
        >
          <span
            class="attention-card__icon grid size-7 place-items-center rounded-xs border border-zinc-200 bg-zinc-50 text-zinc-500 [&_.app-icon]:size-3.5"
            aria-hidden="true"
          >
            <AppIcon :name="item.variant === 'review' ? 'file-text' : 'file'" />
          </span>

          <div class="attention-card__identity grid min-w-0 gap-1">
            <strong
              class="overflow-hidden text-[13px] leading-[19.5px] font-medium text-ellipsis whitespace-nowrap text-zinc-900 transition-colors group-hover:text-brand-600"
            >
              {{ item.title }}
            </strong>
            <div
              class="attention-card__metadata flex min-w-0 items-center gap-2 text-[11px] leading-[16.5px] text-zinc-500 max-[700px]:flex-wrap"
            >
              <span class="rounded-xs bg-zinc-100 px-1.5 py-0.5 text-zinc-600">
                {{ item.category }}
              </span>
              <span
                class="attention-card__confidence inline-flex items-center gap-1 rounded-xs border px-1.5 py-0.5 font-medium [&_.app-icon]:size-3"
                :class="
                  item.variant === 'review'
                    ? 'border-brand-100/50 bg-brand-50 text-brand-700'
                    : 'border-[#d1fae5]/50 bg-[#ecfdf5] text-[#047857]'
                "
              >
                <AppIcon name="check-circle-2" />{{ item.confidenceLabel }}
              </span>
              <span v-if="item.variant === 'review'">{{ item.updatedAt }}</span>
              <span
                v-if="item.recommendation"
                class="attention-card__recommendation inline-flex items-center gap-1 [&_.app-icon]:size-3"
              >
                <AppIcon name="bot" />{{ item.recommendation }}
              </span>
            </div>
          </div>

          <button
            type="button"
            class="inline-flex h-[30px] min-h-[30px] cursor-pointer items-center gap-1.5 rounded-compact px-3 py-0 text-[12px] leading-none font-medium transition-[opacity,color,background-color,border-color] duration-interaction ease-dm focus-visible:opacity-100 max-[700px]:col-start-2 max-[700px]:justify-self-start [&_.app-icon]:size-3.5"
            :class="
              item.variant === 'review'
                ? 'border border-transparent bg-brand-50 text-brand-600 opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 hover:border-brand-100 hover:bg-brand-100'
                : 'border border-zinc-200 bg-white text-zinc-700 shadow-subtle hover:bg-zinc-50'
            "
            @click="emit('open-item', item)"
          >
            {{ item.actionLabel }}
            <AppIcon v-if="item.variant === 'review'" name="arrow-right" />
          </button>
        </div>

        <dl
          v-if="item.extractionFields"
          class="attention-card__fields mb-2.5 grid grid-cols-4 gap-3 rounded-xs border border-zinc-100 bg-zinc-50/80 p-2.5 text-[12px] leading-[18px] max-[700px]:grid-cols-1"
        >
          <div
            v-for="(field, fieldIndex) in item.extractionFields"
            :key="field.label"
            class="grid min-w-0 gap-0.5"
            :class="{ 'col-span-2 max-[700px]:col-span-1': fieldIndex === 1 }"
          >
            <dt class="text-[10px] leading-[15px] tracking-wider text-zinc-400 uppercase">
              {{ field.label }}
            </dt>
            <dd
              class="m-0 overflow-hidden text-[12px] leading-[18px] font-medium text-ellipsis whitespace-nowrap text-zinc-900"
              :class="{ 'font-mono': field.isMonospace }"
            >
              {{ field.value }}
            </dd>
          </div>
        </dl>

        <div
          v-if="item.riskNotice"
          class="attention-card__risk flex items-start gap-2 rounded-xs border border-[#fef3c7]/50 bg-[#fffbeb]/60 p-2 text-[#92400e]"
          role="status"
        >
          <AppIcon class="mt-0.5 size-3.5 shrink-0 text-[#d97706]" name="shield-alert" />
          <p class="m-0 text-[11px] leading-[18px]">
            <strong class="text-[12px] font-medium text-[#78350f]">
              {{ item.riskNotice.title }}
            </strong>
            {{ item.riskNotice.beforeHighlight
            }}<mark
              class="rounded-xs border border-[#fef3c7] bg-white px-1 font-mono font-bold text-[#78350f]"
              >{{ item.riskNotice.highlightedValue }}</mark
            >{{ item.riskNotice.afterHighlight }}
          </p>
        </div>
      </article>
    </div>
  </section>
</template>
