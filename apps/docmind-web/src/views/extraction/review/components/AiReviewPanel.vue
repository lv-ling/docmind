<script setup lang="ts">
import { ref } from 'vue';

import AppIcon from '@/components/AppIcon.vue';
import { DmButton, DmInput } from '@/ui';
import { showToast } from '@/ui/toast.js';

import { REVIEW_DOCUMENT, REVIEW_PIPELINE } from '../model/review-workspace.js';

const emit = defineEmits<{ accept: [] }>();
const question = ref('');

const askAi = (): void => {
  if (question.value.trim().length === 0) return;
  showToast('AI 已结合当前文档证据生成分析', { tone: 'info' });
  question.value = '';
};
</script>

<template>
  <section class="relative flex w-[30%] min-w-0 flex-col border-l border-zinc-200 bg-zinc-50/50">
    <header
      class="z-10 flex items-center justify-between border-b border-zinc-200 bg-white px-4 py-2 text-[11px] font-semibold tracking-wider text-zinc-500 uppercase shadow-subtle"
    >
      <span class="flex items-center gap-1.5"
        ><AppIcon name="sparkles" class="size-3.5" />AI 智能分析</span
      >
      <span class="flex items-center gap-1.5 text-[10px] font-medium text-brand-700">
        <span class="relative flex size-1.5">
          <span
            class="absolute inline-flex size-full animate-dm-ping rounded-full bg-brand-400 opacity-75"
          ></span>
          <span class="relative inline-flex size-1.5 rounded-full bg-brand-500"></span>
        </span>
        深度分析中
      </span>
    </header>

    <div class="flex flex-1 flex-col gap-4 overflow-y-auto p-4">
      <section class="rounded-lg border border-zinc-200 bg-white p-3 shadow-subtle">
        <h2 class="mb-2.5 flex items-center gap-1.5 text-[11px] font-medium text-zinc-500">
          <AppIcon name="activity" class="size-3.5" />处理管线状态
        </h2>
        <ol class="flex items-center justify-between text-[11px]">
          <template v-for="(stage, index) in REVIEW_PIPELINE" :key="stage.label">
            <li
              :class="[
                'flex items-center gap-1.5 rounded px-1.5 py-0.5 font-medium',
                stage.state === 'done'
                  ? 'text-emerald-600'
                  : 'border border-brand-100/50 bg-brand-50 text-brand-700',
              ]"
            >
              <AppIcon
                :name="stage.state === 'done' ? 'check-circle-2' : 'loader-2'"
                :class="['size-3.5', stage.state === 'active' && 'animate-dm-spin']"
              />{{ stage.label }}
            </li>
            <span
              v-if="index < REVIEW_PIPELINE.length - 1"
              class="mx-1 h-px min-w-3 flex-1 bg-zinc-200"
            ></span>
          </template>
        </ol>
      </section>

      <section class="rounded-lg border border-zinc-200 bg-white p-3 shadow-subtle">
        <h2 class="mb-3 flex items-center gap-1.5 text-[11px] font-medium text-zinc-500">
          <AppIcon name="file-search" class="size-3.5" />文档语义概览
        </h2>
        <dl class="grid grid-cols-2 gap-x-2 gap-y-3">
          <div>
            <dt class="text-[10px] tracking-wider text-zinc-400 uppercase">识别类型</dt>
            <dd class="mt-0.5 text-[12px] font-medium text-zinc-900">
              {{ REVIEW_DOCUMENT.documentType }}
            </dd>
          </div>
          <div>
            <dt class="text-[10px] tracking-wider text-zinc-400 uppercase">关键主体</dt>
            <dd class="mt-0.5 text-[12px] font-medium text-zinc-900">
              {{ REVIEW_DOCUMENT.parties }}
            </dd>
          </div>
          <div>
            <dt class="text-[10px] tracking-wider text-zinc-400 uppercase">涉及金额</dt>
            <dd class="mt-0.5 font-mono text-[12px] font-medium text-zinc-900">
              {{ REVIEW_DOCUMENT.amount }}
            </dd>
          </div>
          <div>
            <dt class="text-[10px] tracking-wider text-zinc-400 uppercase">核心标的</dt>
            <dd class="mt-0.5 text-[12px] font-medium text-zinc-900">
              {{ REVIEW_DOCUMENT.subject }}
            </dd>
          </div>
        </dl>
      </section>

      <section
        class="flex flex-col overflow-hidden rounded-lg border border-amber-200/80 bg-white shadow-subtle"
      >
        <header class="flex items-start gap-2 border-b border-amber-100 bg-amber-50/50 px-3 py-2.5">
          <AppIcon name="shield-alert" class="mt-0.5 size-4 shrink-0 text-amber-600" />
          <div>
            <h2 class="text-[12px] font-semibold text-amber-900">发现违约金风险</h2>
            <p class="mt-0.5 text-[11px] text-amber-700">违约金比例超出企业法务标准阈值。</p>
          </div>
        </header>
        <div class="space-y-3 p-3">
          <div class="rounded border border-zinc-100 bg-zinc-50 p-2 text-[11px]">
            <p class="mb-1 flex items-center gap-1 text-zinc-400">
              <AppIcon name="git-compare" class="size-3" />规则比对
            </p>
            <div class="mb-1 flex items-center justify-between">
              <span class="text-zinc-600">企业法务标准</span
              ><span class="rounded border border-zinc-200 bg-white px-1 font-mono text-zinc-900"
                >≤ 20%</span
              >
            </div>
            <div class="flex items-center justify-between">
              <span class="text-zinc-600">当前文档提取</span
              ><span
                class="rounded border border-amber-100 bg-amber-50 px-1 font-mono font-medium text-amber-600"
                >30%</span
              >
            </div>
          </div>
          <div>
            <p class="mb-1 flex items-center gap-1 text-[10px] text-zinc-400">
              <AppIcon name="text-quote" class="size-3" />原文依据
            </p>
            <p
              class="rounded-r border-l-2 border-amber-300 py-0.5 pl-2 text-[11px] leading-relaxed text-zinc-700"
            >
              “第六条违约责任……违约方应支付相当于违约金额
              <mark class="rounded bg-amber-100 px-1 font-medium text-amber-800">30%</mark>
              的违约金。”
            </p>
          </div>
        </div>
        <footer class="mt-2 flex flex-col gap-2 border-t border-zinc-100 bg-zinc-50/50 p-3">
          <p class="text-[10px] font-medium text-zinc-400">AI 推荐操作：</p>
          <div class="flex gap-2">
            <DmButton variant="secondary" class="flex-1" @click="emit('accept')">
              修正为 20%
            </DmButton>
            <DmButton variant="secondary">保留原文</DmButton>
          </div>
          <DmButton variant="accent-ghost" class="mt-1 w-full">
            查看法务规则详情<AppIcon name="chevron-right" class="size-3" />
          </DmButton>
        </footer>
      </section>
    </div>

    <form class="shrink-0 border-t border-zinc-200 bg-white p-3" @submit.prevent="askAi">
      <label class="group relative flex items-center">
        <span class="dm-sr-only">向 AI 提问</span>
        <AppIcon
          name="sparkles"
          class="absolute left-2.5 size-3.5 text-zinc-400 group-focus-within:text-brand-500"
        />
        <DmInput
          v-model="question"
          appearance="unstyled"
          type="text"
          placeholder="向 AI 提问关于此文档..."
          class="h-9 w-full rounded-md border border-zinc-200 bg-zinc-50 pr-10 pl-7 text-[12px] text-zinc-900 shadow-subtle outline-none placeholder:text-zinc-400 focus:border-brand-400 focus:ring-2 focus:ring-brand-100/50"
        />
        <DmButton
          type="submit"
          variant="ghost"
          icon-only
          class="absolute top-[3px] right-1"
          aria-label="提交问题"
        >
          <AppIcon name="arrow-up" class="size-3.5" />
        </DmButton>
      </label>
    </form>
  </section>
</template>
