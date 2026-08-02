<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import { RouteName } from '@/router/constants.js';
import { DmButton, DmInteractiveSurface, DmStatus, DmTextField } from '@/ui';
import { showToast } from '@/ui/toast.js';

import { TEMPLATE_CENTER_ITEMS } from './model/template-center.js';

const router = useRouter();
const isCreateOpen = ref(false);
const templateName = ref('');

const createTemplate = (): void => {
  if (templateName.value.trim().length === 0) return;
  isCreateOpen.value = false;
  showToast(`模板「${templateName.value.trim()}」草稿已创建`);
  templateName.value = '';
};

const openTemplate = async (templateId: string): Promise<void> => {
  await router.push({ name: RouteName.TemplateEditor, query: { templateId } });
};
</script>

<template>
  <section class="flex min-h-full animate-dm-page-fade flex-col bg-zinc-50/30 text-zinc-900">
    <header
      class="sticky top-0 z-10 flex h-16 box-border items-center justify-between border-b border-zinc-200 bg-white/90 px-6 py-4 backdrop-blur-sm"
    >
      <div>
        <h1 class="text-[15px] leading-5 font-semibold">抽取模板</h1>
        <p class="mt-0.5 text-[11px] text-zinc-500">复用稳定的文档结构与字段抽取能力</p>
      </div>
      <DmButton variant="secondary" @click="isCreateOpen = true"
        ><AppIcon name="plus" />新建模板</DmButton
      >
    </header>

    <div
      class="mx-auto grid w-full max-w-[1600px] grid-cols-1 gap-5 p-6 md:grid-cols-2 xl:grid-cols-3"
    >
      <DmInteractiveSurface
        v-for="template in TEMPLATE_CENTER_ITEMS"
        :key="template.id"
        type="button"
        class="group flex min-h-60 flex-col rounded-lg border border-zinc-200 bg-white p-5 text-left shadow-subtle transition-all duration-interaction hover:-translate-y-0.5 hover:border-zinc-300 hover:shadow-card"
        @click="openTemplate(template.id)"
      >
        <div class="mb-4 flex items-start justify-between">
          <span
            :class="[
              'flex size-9 items-center justify-center rounded-md border',
              template.icon === 'layers'
                ? 'border-brand-100 bg-brand-50 text-brand-600'
                : 'border-zinc-200 bg-zinc-100 text-zinc-600',
            ]"
            ><AppIcon :name="template.icon" class="size-4"
          /></span>
          <DmStatus
            :label="template.status"
            :tone="template.status === '启用中' ? 'success' : 'warning'"
          />
        </div>
        <h2
          class="mb-1.5 text-[14px] font-semibold text-zinc-900 transition-colors group-hover:text-brand-600"
        >
          {{ template.name }}
        </h2>
        <p class="mb-5 flex-1 text-[11px] leading-relaxed text-zinc-500">
          {{ template.description }}
        </p>
        <dl
          class="grid grid-cols-2 gap-3 rounded border border-zinc-100 bg-zinc-50 p-3 text-[11px]"
        >
          <div>
            <dt class="mb-1 text-[10px] tracking-wider text-zinc-400 uppercase">定义字段</dt>
            <dd class="font-mono font-medium text-zinc-900">{{ template.fieldCount }} 个</dd>
          </div>
          <div>
            <dt class="mb-1 text-[10px] tracking-wider text-zinc-400 uppercase">历史应用</dt>
            <dd class="font-mono font-medium text-zinc-900">{{ template.usageCount }}</dd>
          </div>
        </dl>
        <footer class="mt-3 flex items-center justify-between text-[10px] text-zinc-400">
          <span>更新于 {{ template.updatedAt }}</span
          ><span
            class="flex items-center gap-1 font-medium text-brand-600 opacity-0 transition-opacity group-hover:opacity-100"
            >打开模板<AppIcon name="arrow-right" class="size-3"
          /></span>
        </footer>
      </DmInteractiveSurface>
    </div>

    <div
      v-if="isCreateOpen"
      class="fixed inset-0 z-50 grid place-items-center bg-zinc-950/35 p-6 backdrop-blur-[2px]"
      @click.self="isCreateOpen = false"
    >
      <form
        class="w-full max-w-md rounded-lg border border-zinc-200 bg-white p-5 shadow-float"
        @submit.prevent="createTemplate"
      >
        <header class="mb-5 flex items-start justify-between">
          <div>
            <h2 class="text-[14px] font-semibold text-zinc-900">新建抽取模板</h2>
            <p class="mt-1 text-[11px] text-zinc-500">创建草稿后进入模板编辑器配置字段。</p>
          </div>
          <DmButton
            variant="ghost"
            icon-only
            aria-label="关闭新建模板窗口"
            @click="isCreateOpen = false"
          >
            <AppIcon name="close" class="size-4" />
          </DmButton>
        </header>
        <DmTextField
          id="template-name"
          v-model="templateName"
          label="模板名称"
          placeholder="例如：标准采购合同 v3"
          autofocus
          :maxlength="80"
          required
        />
        <footer class="mt-6 flex justify-end gap-2">
          <DmButton variant="secondary" @click="isCreateOpen = false">取消</DmButton
          ><DmButton type="submit">创建草稿</DmButton>
        </footer>
      </form>
    </div>
  </section>
</template>
