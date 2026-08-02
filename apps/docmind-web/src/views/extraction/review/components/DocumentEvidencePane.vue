<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue';

import { REVIEW_DOCUMENT } from '../model/review-workspace.js';

defineProps<{ activeFieldId: string }>();
</script>

<template>
  <section class="flex w-[42%] min-w-0 flex-col border-r border-zinc-200 bg-zinc-100/50">
    <header
      class="z-10 flex items-center justify-between border-b border-zinc-200 bg-white px-4 py-2 text-[11px] font-semibold tracking-wider text-zinc-500 uppercase shadow-subtle"
    >
      <span class="flex items-center gap-1.5"><AppIcon name="eye" class="size-3.5" />原文查验</span>
      <span>{{ REVIEW_DOCUMENT.page }} / {{ REVIEW_DOCUMENT.pageCount }} 页</span>
    </header>
    <div class="flex flex-1 justify-center overflow-y-auto p-6">
      <article
        class="min-h-max w-full max-w-lg border border-zinc-200 bg-white p-10 font-document text-[13px] leading-loose text-zinc-800 shadow-card"
      >
        <header class="mx-12 mb-8 border-b-2 border-zinc-900 pb-2 text-center">
          <h1 class="font-ui text-lg font-semibold tracking-widest text-zinc-900">采购框架协议</h1>
        </header>
        <p>
          本协议由以下双方于
          <mark
            :class="[
              'rounded px-1 py-0.5 text-inherit transition-colors',
              activeFieldId === 'signDate' ? 'bg-brand-100 ring-1 ring-brand-400' : 'bg-brand-50',
            ]"
            >2023年1月1日</mark
          >
          签署：
        </p>
        <div class="my-4 space-y-2 font-ui">
          <p>
            <strong>甲方：</strong>
            <mark
              :class="[
                'rounded px-1 py-0.5 text-inherit transition-colors',
                activeFieldId === 'partyA' ? 'bg-brand-100 ring-1 ring-brand-400' : 'bg-brand-50',
              ]"
              >北京字节跳动科技有限公司</mark
            >
          </p>
          <p>
            <strong>乙方：</strong>
            <mark
              :class="[
                'rounded px-1 py-0.5 text-inherit transition-colors',
                activeFieldId === 'partyB' ? 'bg-brand-100 ring-1 ring-brand-400' : 'bg-brand-50',
              ]"
              >上海微盟企业发展有限公司</mark
            >
          </p>
        </div>
        <p class="mt-6 indent-8">
          鉴于甲方业务发展需要，拟向乙方采购相关服务……合同总金额预计为
          <mark
            :class="[
              'rounded px-1 py-0.5 font-ui font-medium text-inherit transition-colors',
              activeFieldId === 'amount' ? 'bg-brand-100 ring-1 ring-brand-400' : 'bg-brand-50',
            ]"
            >¥ 5,000,000.00</mark
          >
          元。
        </p>
        <section class="mt-8">
          <h2 class="font-ui font-semibold">第六条 违约责任</h2>
          <p class="mt-2 indent-8">
            若任何一方违反本协议约定，违约方应支付相当于违约金额
            <mark
              :class="[
                'rounded px-1 py-0.5 font-ui font-medium text-amber-900 transition-all',
                activeFieldId === 'penalty'
                  ? 'bg-amber-200 ring-2 ring-amber-400 ring-offset-1'
                  : 'bg-amber-100 ring-1 ring-amber-300',
              ]"
              >30%</mark
            >
            的违约金。
          </p>
        </section>
        <section class="mt-8 border-t border-zinc-200 pt-5">
          <h2 class="font-ui font-semibold">第七条 保密义务</h2>
          <p class="mt-2 indent-8">双方对履行本协议过程中知悉的商业秘密负有持续保密义务。</p>
        </section>
      </article>
    </div>
  </section>
</template>
