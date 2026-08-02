<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import { RouteName } from '@/router/constants.js';
import { DmButton } from '@/ui';
import { showToast } from '@/ui/toast.js';

import { REVIEW_QUEUE_ITEMS, type ReviewQueueItem } from './model/review-queue.js';

const router = useRouter();
const archivedIds = ref<string[]>([]);
const activeItems = computed(() =>
  REVIEW_QUEUE_ITEMS.filter((item) => !archivedIds.value.includes(item.id)),
);
const highRiskItems = computed(() => activeItems.value.filter((item) => item.riskLevel === 'high'));
const normalItems = computed(() => activeItems.value.filter((item) => item.riskLevel === 'normal'));

const openReview = async (item: ReviewQueueItem): Promise<void> => {
  await router.push({ name: RouteName.ExtractionReview, query: { extractionId: item.id } });
};

const archiveItem = (item: ReviewQueueItem): void => {
  archivedIds.value = [...archivedIds.value, item.id];
  showToast(`${item.title} 已归档`);
};
</script>

<template>
  <section class="flex min-h-full animate-dm-page-fade flex-col bg-zinc-50/30 text-zinc-900">
    <header
      class="sticky top-0 z-10 flex h-16 box-border items-center justify-between border-b border-zinc-200 bg-white/90 px-6 py-4 backdrop-blur-sm"
    >
      <div>
        <h1 class="text-[15px] leading-5 font-semibold">审核中心</h1>
        <p class="mt-0.5 text-[11px] text-zinc-500">按风险优先级完成文档人工复核</p>
      </div>
      <DmButton
        variant="secondary"
        :disabled="normalItems.length === 0"
        @click="normalItems.forEach(archiveItem)"
      >
        全部通过 ({{ normalItems.length }})
      </DmButton>
    </header>

    <div class="mx-auto w-full max-w-[1600px] space-y-8 p-6">
      <section>
        <header class="mb-4 flex items-center gap-2 border-b border-zinc-200 pb-2">
          <h2 class="flex items-center gap-1.5 text-[13px] font-semibold text-amber-800">
            <AppIcon name="shield-alert" class="size-4 text-amber-600" />高风险需干预
          </h2>
          <span
            class="rounded border border-amber-200 bg-amber-100/80 px-1.5 py-0.5 text-[10px] font-bold text-amber-800"
            >{{ highRiskItems.length }} 份</span
          >
        </header>
        <div class="space-y-3">
          <article
            v-for="item in highRiskItems"
            :key="item.id"
            class="group relative flex cursor-pointer items-center justify-between overflow-hidden rounded-lg border border-amber-200/70 bg-white p-4 shadow-subtle transition-all hover:-translate-y-0.5 hover:shadow-card"
            tabindex="0"
            @click="openReview(item)"
            @keydown.enter="openReview(item)"
          >
            <span class="absolute inset-y-0 left-0 w-1 bg-amber-400"></span>
            <div class="ml-1 flex min-w-0 items-start gap-4">
              <span
                class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded border border-amber-100 bg-amber-50"
              >
                <AppIcon name="file-warning" class="size-4 text-amber-600" />
              </span>
              <div class="min-w-0">
                <h3
                  class="truncate text-[13px] font-medium text-zinc-900 transition-colors group-hover:text-amber-700"
                >
                  {{ item.title }}
                </h3>
                <div class="my-2 flex items-center gap-3 text-[11px] text-zinc-500">
                  <span
                    class="rounded border border-brand-100/50 bg-brand-50 px-1 font-medium text-brand-700"
                    >置信度 {{ item.confidence }}%</span
                  ><span class="size-1 rounded-full bg-zinc-300"></span
                  ><span>{{ item.category }}</span
                  ><span class="size-1 rounded-full bg-zinc-300"></span
                  ><span>{{ item.updatedAt }}</span>
                </div>
                <div
                  class="inline-flex items-center gap-1.5 rounded border border-amber-100/60 bg-amber-50 px-2 py-1 text-[11px] text-amber-800"
                >
                  <AppIcon name="alert-triangle" class="size-3 text-amber-600" />{{ item.detail }}
                </div>
              </div>
            </div>
            <div class="ml-6 flex shrink-0 flex-col items-end gap-2">
              <DmButton variant="dark" @click.stop="openReview(item)"
                >进入复核<AppIcon name="arrow-right"
              /></DmButton>
              <span class="flex items-center gap-1 text-[10px] text-zinc-400"
                ><AppIcon name="bot" class="size-3" />{{ item.suggestion }}</span
              >
            </div>
          </article>
        </div>
      </section>

      <section>
        <header class="mb-4 flex items-center gap-2 border-b border-zinc-200 pb-2">
          <h2 class="flex items-center gap-1.5 text-[13px] font-semibold text-zinc-800">
            <AppIcon name="list-checks" class="size-4 text-zinc-500" />常规审核
          </h2>
          <span
            class="rounded border border-zinc-200 bg-zinc-100 px-1.5 py-0.5 text-[10px] font-bold text-zinc-600"
            >{{ normalItems.length }} 份</span
          >
        </header>
        <div class="space-y-3">
          <article
            v-for="item in normalItems"
            :key="item.id"
            class="group flex cursor-pointer items-center justify-between rounded-lg border border-zinc-200 bg-white p-4 shadow-subtle transition-all hover:-translate-y-0.5 hover:shadow-card"
            tabindex="0"
            @click="openReview(item)"
            @keydown.enter="openReview(item)"
          >
            <div class="flex min-w-0 items-start gap-4">
              <span
                class="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded border border-zinc-200 bg-zinc-50 transition-colors group-hover:border-brand-200 group-hover:bg-brand-50"
              >
                <AppIcon name="file-text" class="size-4 text-zinc-400 group-hover:text-brand-600" />
              </span>
              <div class="min-w-0">
                <h3
                  class="truncate text-[13px] font-medium text-zinc-900 transition-colors group-hover:text-brand-600"
                >
                  {{ item.title }}
                </h3>
                <div class="my-2 flex items-center gap-3 text-[11px] text-zinc-500">
                  <span
                    class="rounded border border-emerald-100/50 bg-emerald-50 px-1 font-medium text-emerald-700"
                    >置信度 {{ item.confidence }}%</span
                  ><span class="size-1 rounded-full bg-zinc-300"></span
                  ><span>{{ item.category }}</span
                  ><span class="size-1 rounded-full bg-zinc-300"></span
                  ><span>{{ item.updatedAt }}</span>
                </div>
                <div
                  class="inline-flex items-center gap-1.5 rounded border border-zinc-200/80 bg-zinc-50 px-2 py-1 text-[11px] text-zinc-600"
                >
                  <AppIcon name="info" class="size-3 text-zinc-400" />{{ item.detail }}
                </div>
              </div>
            </div>
            <div class="ml-6 flex shrink-0 items-center gap-3">
              <DmButton variant="secondary" @click.stop="archiveItem(item)">快捷归档</DmButton>
              <DmButton variant="accent" @click.stop="openReview(item)"
                >复核<AppIcon name="arrow-right"
              /></DmButton>
            </div>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>
