<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';

import type { WorkbenchEfficiency, WorkbenchInsight } from '../model/workbench-overview.js';

defineProps<{
  efficiency: WorkbenchEfficiency;
  insight: WorkbenchInsight;
}>();

const emit = defineEmits<{
  'open-config': [];
}>();
</script>

<template>
  <aside
    class="workspace-insights col-span-4 grid content-start gap-5 max-[1050px]:col-span-1"
    aria-label="AI 效能与建议"
  >
    <section
      class="workspace-insights__dark relative overflow-hidden rounded-md border border-zinc-800 bg-zinc-900 p-5 text-zinc-100 shadow-[0_4px_6px_-1px_rgb(0_0_0_/_10%),0_2px_4px_-2px_rgb(0_0_0_/_10%)]"
    >
      <span
        class="workspace-insights__glow pointer-events-none absolute -top-10 -right-10 size-40 rounded-full bg-brand-500/20 blur-3xl"
        aria-hidden="true"
      ></span>
      <header
        class="relative z-10 mb-4 flex items-center gap-2 text-[12px] leading-[18px] font-medium tracking-wide text-zinc-300"
      >
        <AppIcon class="size-3.5 text-brand-400" name="activity" />
        <span>AI 效能观测</span>
      </header>

      <div class="workspace-insights__headline relative z-10 flex items-baseline gap-1.5">
        <strong class="font-mono text-3xl leading-9 font-semibold tracking-tight text-white">
          {{ efficiency.parsedDocumentCount }}
        </strong>
        <span class="text-[11px] leading-[17.875px] text-zinc-400">份文档已解析</span>
      </div>
      <p class="relative z-10 mt-1 mb-3 w-[90%] text-[11px] leading-[17.875px] text-zinc-400">
        本周自动抽取已为您节省约
        <strong class="font-medium text-brand-300">{{ efficiency.savedHours }} 小时</strong>
        工时。
      </p>

      <dl class="relative z-10 m-0 grid grid-cols-2 gap-2 border-t border-zinc-800/80 pt-3">
        <div class="grid">
          <dt
            class="mb-0.5 h-[15px] text-[10px] leading-[15px] tracking-wider text-zinc-500 uppercase"
          >
            综合准确率
          </dt>
          <dd class="m-0 h-[19.5px] text-[13px] leading-[19.5px] font-semibold text-zinc-200">
            {{ efficiency.accuracyRate.toFixed(1) }}%
            <span class="text-[10px] text-[#34d399]">↑</span>
          </dd>
        </div>
        <div class="grid">
          <dt
            class="mb-0.5 h-[15px] text-[10px] leading-[15px] tracking-wider text-zinc-500 uppercase"
          >
            自动归档率
          </dt>
          <dd class="m-0 h-[19.5px] text-[13px] leading-[19.5px] font-semibold text-zinc-200">
            {{ efficiency.autoArchiveRate.toFixed(1) }}%
          </dd>
        </div>
      </dl>
    </section>

    <section
      class="workspace-insights__notice relative overflow-hidden rounded-md border border-zinc-200 bg-white p-4 shadow-subtle"
    >
      <header class="mb-3 flex items-center gap-1.5">
        <AppIcon class="size-3.5 text-brand-500" name="lightbulb" />
        <h2 class="m-0 text-[12px] leading-[18px] font-semibold text-zinc-900">全局发现与建议</h2>
      </header>
      <div class="flex items-start gap-2.5">
        <AppIcon class="mt-0.5 size-3.5 shrink-0 text-[#f59e0b]" name="trending-up" />
        <span class="block">
          <strong class="mb-0.5 block text-[12px] leading-[18px] font-medium text-zinc-900">
            {{ insight.title }}
          </strong>
          <p class="m-0 text-[11px] leading-[17.875px] text-zinc-500">
            {{ insight.description }}
          </p>
          <button
            type="button"
            class="mt-1.5 inline-flex cursor-pointer items-center gap-1 border-0 bg-transparent p-0 text-[11px] leading-[16.5px] font-medium text-brand-600 hover:text-brand-700 [&_.app-icon]:size-3"
            @click="emit('open-config')"
          >
            {{ insight.actionLabel }} <AppIcon name="chevron-right" />
          </button>
        </span>
      </div>
    </section>
  </aside>
</template>

<style scoped>
.workspace-insights__notice::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: #818cf8;
  content: '';
}
</style>
