<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';
import { DmInteractiveSurface } from '@/ui';

import type { WorkbenchPipelineItem } from '../model/workbench-overview.js';

defineProps<{ items: WorkbenchPipelineItem[] }>();

const emit = defineEmits<{
  'open-item': [item: WorkbenchPipelineItem];
}>();
</script>

<template>
  <section
    id="workbench-pipeline"
    class="pipeline-panel grid scroll-mt-18 gap-3"
    aria-labelledby="pipeline-title"
  >
    <header>
      <h2 id="pipeline-title" class="m-0 text-[13px] leading-[19.5px] font-semibold text-zinc-800">
        处理管线
      </h2>
    </header>

    <div class="pipeline-panel__list grid grid-cols-2 gap-4 max-[700px]:grid-cols-1">
      <DmInteractiveSurface
        v-for="item in items"
        :key="item.id"
        type="button"
        class="grid min-w-0 cursor-pointer grid-cols-[24px_minmax(0,1fr)] gap-x-3 gap-y-0 rounded-md border border-brand-200/60 bg-brand-50/20 p-3 text-left shadow-subtle transition-[background-color,transform,box-shadow] duration-interaction ease-dm hover:-translate-y-px hover:bg-brand-50/50 hover:shadow-[0_5px_16px_rgb(99_102_241_/_7%)] motion-reduce:transition-none"
        @click="emit('open-item', item)"
      >
        <span
          class="pipeline-panel__engine mt-0.5 grid size-6 place-items-center rounded-xs border border-brand-200 bg-brand-100/50 text-brand-600 [&_.app-icon]:size-3"
          aria-hidden="true"
        >
          <AppIcon name="cpu" />
        </span>
        <span class="pipeline-panel__copy grid min-w-0 gap-0.5">
          <strong
            class="overflow-hidden text-[12px] leading-[18px] font-medium text-ellipsis whitespace-nowrap text-zinc-900"
          >
            {{ item.title }}
          </strong>
          <small
            class="overflow-hidden text-[11px] leading-[16.5px] text-ellipsis whitespace-nowrap text-zinc-500"
          >
            包含 {{ item.documentCount }} 份文件
          </small>
        </span>
        <span
          class="pipeline-panel__status col-span-full mt-3 flex items-center justify-between gap-3 text-[11px] leading-[16.5px] text-zinc-600"
        >
          <span class="inline-flex items-center gap-1 [&_.app-icon]:size-3">
            <AppIcon class="animate-dm-spin" name="loader-2" />{{ item.stageLabel }}
          </span>
          <strong class="font-mono text-[11px] font-medium text-brand-600">
            {{ item.progress }}%
          </strong>
        </span>
        <span
          class="pipeline-panel__progress relative col-span-full mt-1.5 h-1 overflow-hidden rounded-full bg-zinc-200/80"
          aria-hidden="true"
        >
          <i
            class="absolute inset-y-0 left-0 rounded-full bg-brand-500 transition-[width] duration-control ease-dm motion-reduce:transition-none"
            :style="{ width: `${item.progress}%` }"
          ></i>
        </span>
      </DmInteractiveSurface>
    </div>
  </section>
</template>
