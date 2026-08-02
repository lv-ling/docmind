<script setup lang="ts">
import { computed, ref } from 'vue';

import AppIcon from '@/components/AppIcon.vue';
import { DmButton, DmProgress, DmTabs } from '@/ui';
import { showToast } from '@/ui/toast.js';

import {
  PROCESSING_TASKS,
  PROCESSING_TABS,
  type ProcessingTab,
} from './model/processing-center.js';

const activeTab = ref<ProcessingTab>('running');
const visibleTasks = computed(() =>
  PROCESSING_TASKS.filter((task) => task.category === activeTab.value),
);
</script>

<template>
  <section class="flex min-h-full animate-dm-page-fade flex-col bg-white text-zinc-900">
    <header
      class="sticky top-0 z-10 flex h-16 box-border items-center justify-between border-b border-zinc-200 bg-white/90 px-6 py-4 backdrop-blur-sm"
    >
      <div>
        <h1 class="text-[15px] leading-5 font-semibold">AI 处理中心</h1>
        <p class="mt-0.5 text-[11px] text-zinc-500">实时观测文档智能处理管线</p>
      </div>
      <DmButton variant="dark" @click="showToast('新的处理管线已加入队列')">
        <AppIcon name="play" />新建处理管线
      </DmButton>
    </header>

    <div class="mx-auto w-full max-w-[1600px] p-6">
      <DmTabs v-model="activeTab" :items="PROCESSING_TABS" label="处理任务状态" class="mb-6" />

      <div class="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <article
          v-for="task in visibleTasks"
          :key="task.id"
          class="rounded-lg border border-zinc-200 bg-white p-5 shadow-subtle transition-all duration-interaction hover:-translate-y-0.5 hover:border-zinc-300 hover:shadow-card"
        >
          <header class="mb-4 flex items-start justify-between gap-4">
            <div class="min-w-0">
              <div class="mb-1 flex items-center gap-2">
                <span
                  class="rounded border border-brand-100 bg-brand-50 px-1.5 py-0.5 text-[10px] font-medium text-brand-700"
                  >任务 ID: {{ task.id }}</span
                >
                <span class="flex items-center gap-1 text-[11px] text-zinc-500"
                  ><AppIcon name="clock" class="size-3" />{{ task.elapsed }}</span
                >
              </div>
              <h2 class="truncate text-[14px] font-semibold text-zinc-900">{{ task.title }}</h2>
              <p class="mt-0.5 text-[11px] text-zinc-500">{{ task.scope }}</p>
            </div>
            <span v-if="task.category === 'running'" class="relative mt-1 flex size-2 shrink-0">
              <span
                class="absolute inline-flex size-full animate-dm-ping rounded-full bg-brand-400 opacity-60"
              ></span>
              <span class="relative inline-flex size-2 rounded-full bg-brand-500"></span>
            </span>
            <AppIcon
              v-else-if="task.category === 'completed'"
              name="check-circle-2"
              class="size-4 text-emerald-600"
            />
            <AppIcon
              v-else-if="task.category === 'failed'"
              name="alert-triangle"
              class="size-4 text-red-600"
            />
            <AppIcon v-else name="clock" class="size-4 text-zinc-400" />
          </header>

          <section class="mb-4 rounded-md border border-zinc-100 bg-zinc-50 p-3">
            <div class="mb-2 flex items-center justify-between text-[12px]">
              <span class="font-medium text-zinc-700">状态: {{ task.status }}</span>
              <span
                :class="[
                  'font-mono font-medium',
                  task.category === 'failed' ? 'text-red-600' : 'text-brand-600',
                ]"
                >{{ task.progress }}%</span
              >
            </div>
            <DmProgress
              :value="task.progress"
              size="small"
              :tone="
                task.category === 'failed'
                  ? 'danger'
                  : task.category === 'completed'
                    ? 'success'
                    : 'brand'
              "
            />
            <p class="mt-2 truncate text-[11px] text-zinc-500">{{ task.current }}</p>
          </section>

          <ol
            class="flex items-center justify-between gap-1 rounded border border-zinc-100 bg-white p-2.5 text-[11px] text-zinc-600"
            aria-label="处理管线"
          >
            <template v-for="(stage, index) in task.stages" :key="stage.label">
              <li
                :class="[
                  'flex shrink-0 items-center gap-1 rounded px-1 py-0.5',
                  stage.state === 'done' && 'text-emerald-600',
                  stage.state === 'active' &&
                    'border border-brand-100 bg-brand-50 font-medium text-brand-700 shadow-subtle',
                  stage.state === 'failed' &&
                    'border border-red-100 bg-red-50 font-medium text-red-600',
                  stage.state === 'pending' && 'text-zinc-400',
                ]"
              >
                <AppIcon
                  :name="
                    stage.state === 'done'
                      ? 'check-circle-2'
                      : stage.state === 'active'
                        ? 'loader-2'
                        : stage.state === 'failed'
                          ? 'alert-triangle'
                          : 'circle'
                  "
                  :class="['size-3.5', stage.state === 'active' && 'animate-dm-spin']"
                />
                {{ stage.label }}
              </li>
              <span
                v-if="index < task.stages.length - 1"
                class="h-px min-w-2 flex-1 bg-zinc-200"
              ></span>
            </template>
          </ol>
        </article>
      </div>

      <div
        v-if="visibleTasks.length === 0"
        class="rounded-lg border border-dashed border-zinc-200 py-16 text-center text-[12px] text-zinc-500"
      >
        当前状态下暂无处理任务
      </div>
    </div>
  </section>
</template>
