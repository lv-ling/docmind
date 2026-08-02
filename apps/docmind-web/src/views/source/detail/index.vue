<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import { RouteName } from '@/router/constants.js';
import { DmButton, DmInteractiveSurface, DmStatus } from '@/ui';
import { showToast } from '@/ui/toast.js';

import { DOCUMENT_DETAIL_MODEL } from './model/document-detail.js';

const router = useRouter();
const selectedVersionId = ref('v3');

const startExtraction = async (): Promise<void> => {
  await router.push({ name: RouteName.ExtractionProcessing });
};
</script>

<template>
  <section class="flex min-h-full animate-dm-page-fade flex-col bg-zinc-50/30 text-zinc-900">
    <header
      class="sticky top-0 z-10 flex h-16 box-border items-center justify-between border-b border-zinc-200 bg-white/90 px-6 py-3 backdrop-blur-sm"
    >
      <div class="flex min-w-0 items-center gap-3">
        <DmButton
          variant="ghost"
          icon-only
          class="shrink-0"
          aria-label="返回文档中心"
          @click="router.push({ name: RouteName.SourceList })"
        >
          <AppIcon name="arrow-left" class="size-4" />
        </DmButton>
        <span class="h-4 w-px bg-zinc-200"></span>
        <div class="min-w-0">
          <h1 class="truncate text-[15px] font-semibold text-zinc-900">
            {{ DOCUMENT_DETAIL_MODEL.name }}
          </h1>
          <p class="mt-0.5 text-[10px] text-zinc-400">
            不可变原件 · {{ DOCUMENT_DETAIL_MODEL.checksum }}
          </p>
        </div>
      </div>
      <div class="ml-4 flex shrink-0 items-center gap-2">
        <DmButton variant="secondary" @click="showToast('模板关联设置已打开', { tone: 'info' })"
          ><AppIcon name="layers" />关联模板</DmButton
        >
        <DmButton variant="dark" @click="startExtraction"
          ><AppIcon name="play" />发起 AI 处理</DmButton
        >
      </div>
    </header>

    <div class="mx-auto grid w-full max-w-[1600px] grid-cols-[minmax(0,1fr)_340px] gap-5 p-6">
      <section
        class="min-w-0 overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle"
      >
        <header
          class="flex items-center justify-between border-b border-zinc-200 bg-zinc-50/80 px-4 py-2.5"
        >
          <div
            class="flex items-center gap-2 text-[11px] font-semibold tracking-wider text-zinc-500 uppercase"
          >
            <AppIcon name="eye" class="size-3.5" />文档预览
          </div>
          <div class="flex items-center gap-3 text-[10px] text-zinc-400">
            <span>{{ DOCUMENT_DETAIL_MODEL.pages }} 页</span
            ><span>{{ DOCUMENT_DETAIL_MODEL.size }}</span
            ><span>{{ DOCUMENT_DETAIL_MODEL.fileType }}</span>
          </div>
        </header>
        <div class="flex min-h-[650px] justify-center overflow-auto bg-zinc-100/70 p-8">
          <article
            class="min-h-max w-full max-w-2xl border border-zinc-200 bg-white px-14 py-12 font-document text-[13px] leading-8 text-zinc-800 shadow-card"
          >
            <header class="mx-16 mb-10 border-b-2 border-zinc-900 pb-3 text-center">
              <h2 class="font-ui text-xl font-semibold tracking-[0.2em] text-zinc-900">
                采购框架协议
              </h2>
              <p class="mt-2 font-ui text-[10px] tracking-wider text-zinc-400">
                文件编号：DM-CG-2023-001
              </p>
            </header>
            <p>本协议由以下双方于 2023年1月1日 签署：</p>
            <div class="my-5 space-y-2 font-ui">
              <p><strong>甲方：</strong>北京字节跳动科技有限公司</p>
              <p><strong>乙方：</strong>上海微盟企业发展有限公司</p>
            </div>
            <p class="mt-6 indent-8">
              鉴于甲方业务发展需要，拟向乙方采购相关技术服务。双方本着平等、自愿、诚实信用原则，就相关合作事项达成本框架协议。
            </p>
            <section class="mt-8">
              <h3 class="font-ui font-semibold">第一条 合作内容</h3>
              <p class="mt-2 indent-8">
                乙方依据具体采购订单向甲方提供技术服务、实施支持及相关交付成果。
              </p>
            </section>
            <section class="mt-8">
              <h3 class="font-ui font-semibold">第二条 合同金额</h3>
              <p class="mt-2 indent-8">
                本框架协议项下合同总金额预计为
                <mark class="rounded bg-brand-50 px-1 font-ui font-medium text-brand-800"
                  >¥ 5,000,000.00</mark
                >
                元。
              </p>
            </section>
            <section class="mt-8">
              <h3 class="font-ui font-semibold">第六条 违约责任</h3>
              <p class="mt-2 indent-8">
                若任何一方违反本协议约定，违约方应支付相当于违约金额
                <mark class="rounded bg-amber-100 px-1 font-ui font-medium text-amber-900"
                  >30%</mark
                >
                的违约金。
              </p>
            </section>
          </article>
        </div>
      </section>

      <aside class="space-y-4">
        <section class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
          <header class="border-b border-zinc-200 bg-zinc-50/80 px-4 py-3">
            <h2 class="text-[12px] font-semibold text-zinc-900">文档信息</h2>
          </header>
          <dl class="grid grid-cols-2 gap-x-4 gap-y-3 p-4 text-[11px]">
            <div>
              <dt class="text-zinc-400">分类</dt>
              <dd class="mt-1 font-medium text-zinc-900">{{ DOCUMENT_DETAIL_MODEL.category }}</dd>
            </div>
            <div>
              <dt class="text-zinc-400">上传人</dt>
              <dd class="mt-1 font-medium text-zinc-900">{{ DOCUMENT_DETAIL_MODEL.uploader }}</dd>
            </div>
            <div>
              <dt class="text-zinc-400">上传时间</dt>
              <dd class="mt-1 font-medium text-zinc-900">{{ DOCUMENT_DETAIL_MODEL.uploadedAt }}</dd>
            </div>
            <div>
              <dt class="text-zinc-400">文件规格</dt>
              <dd class="mt-1 font-medium text-zinc-900">
                {{ DOCUMENT_DETAIL_MODEL.size }} · {{ DOCUMENT_DETAIL_MODEL.pages }} 页
              </dd>
            </div>
          </dl>
        </section>

        <section class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
          <header
            class="flex items-center justify-between border-b border-zinc-200 bg-zinc-50/80 px-4 py-3"
          >
            <h2 class="text-[12px] font-semibold text-zinc-900">AI 处理记录</h2>
            <DmStatus label="待审核" tone="warning" />
          </header>
          <ol class="p-4">
            <li
              v-for="record in DOCUMENT_DETAIL_MODEL.processingRecords"
              :key="record.label"
              class="relative flex gap-3 pb-4 last:pb-0"
            >
              <span class="absolute top-4 bottom-0 left-[7px] w-px bg-zinc-200 last:hidden"></span>
              <AppIcon
                :name="record.status === 'warning' ? 'alert-triangle' : 'check-circle-2'"
                :class="[
                  'relative z-10 size-4 shrink-0 bg-white',
                  record.status === 'warning' ? 'text-amber-600' : 'text-emerald-600',
                ]"
              />
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between">
                  <strong class="text-[11px] font-medium text-zinc-900">{{ record.label }}</strong
                  ><span class="text-[10px] text-zinc-400">{{ record.time }}</span>
                </div>
                <p class="mt-1 text-[10px] leading-4 text-zinc-500">{{ record.detail }}</p>
              </div>
            </li>
          </ol>
        </section>

        <section class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
          <header class="border-b border-zinc-200 bg-zinc-50/80 px-4 py-3">
            <h2 class="text-[12px] font-semibold text-zinc-900">关联模板</h2>
          </header>
          <DmInteractiveSurface
            type="button"
            class="group flex w-full items-center gap-3 p-4 text-left hover:bg-zinc-50"
            @click="router.push({ name: RouteName.TemplateList })"
          >
            <span
              class="grid size-8 place-items-center rounded border border-brand-100 bg-brand-50 text-brand-600"
              ><AppIcon name="layers" class="size-4" /></span
            ><span class="min-w-0 flex-1"
              ><strong class="block truncate text-[12px] font-medium text-zinc-900">{{
                DOCUMENT_DETAIL_MODEL.template
              }}</strong
              ><small class="mt-0.5 text-[10px] text-emerald-600">{{
                DOCUMENT_DETAIL_MODEL.templateStatus
              }}</small></span
            ><AppIcon
              name="chevron-right"
              class="size-3.5 text-zinc-300 group-hover:text-brand-500"
            />
          </DmInteractiveSurface>
        </section>

        <section class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
          <header class="border-b border-zinc-200 bg-zinc-50/80 px-4 py-3">
            <h2 class="text-[12px] font-semibold text-zinc-900">版本信息</h2>
          </header>
          <div class="p-2">
            <DmInteractiveSurface
              v-for="version in DOCUMENT_DETAIL_MODEL.versions"
              :key="version.id"
              type="button"
              :class="[
                'flex w-full items-center gap-3 rounded-md p-2 text-left transition-colors',
                selectedVersionId === version.id ? 'bg-brand-50' : 'hover:bg-zinc-50',
              ]"
              @click="selectedVersionId = version.id"
            >
              <span
                :class="[
                  'rounded border px-1.5 py-0.5 font-mono text-[10px] font-medium',
                  selectedVersionId === version.id
                    ? 'border-brand-200 bg-white text-brand-700'
                    : 'border-zinc-200 text-zinc-500',
                ]"
                >{{ version.label }}</span
              ><span class="min-w-0 flex-1"
                ><strong class="block truncate text-[11px] font-medium text-zinc-800">{{
                  version.fileName
                }}</strong
                ><small class="text-[10px] text-zinc-400">{{ version.date }}</small></span
              ><span class="text-[10px] text-zinc-400">{{ version.status }}</span>
            </DmInteractiveSurface>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>
