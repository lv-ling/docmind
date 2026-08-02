<script setup lang="ts">
import { computed, ref } from 'vue';

import AppIcon from '@/components/AppIcon.vue';
import {
  DmButton,
  DmCheckbox,
  DmInput,
  DmInteractiveSurface,
  DmRange,
  DmSelect,
  DmStatus,
  DmTabs,
} from '@/ui';
import { showToast } from '@/ui/toast.js';

import {
  CONFIG_TABS,
  DEFAULT_SCHEMA_FIELDS,
  SCHEMA_LIST,
  SENSITIVE_RULES,
  type ConfigTab,
} from './model/config-center.js';

const activeTab = ref<ConfigTab>('schema');
const selectedSchemaId = ref('schema-purchase');
const fields = ref(DEFAULT_SCHEMA_FIELDS.map((field) => ({ ...field })));
const sensitiveRules = ref(SENSITIVE_RULES.map((rule) => ({ ...rule })));
const autoApproveThreshold = ref(95);
const humanReviewThreshold = ref(80);
const selectedSchema = computed(() =>
  SCHEMA_LIST.find((schema) => schema.id === selectedSchemaId.value),
);

const addField = (): void => {
  const index = fields.value.length + 1;
  fields.value.push({
    id: `field-${Date.now()}`,
    name: `自定义字段 ${index}`,
    key: `custom.field_${index}`,
    type: '文本',
    required: false,
  });
};

const saveConfiguration = (): void => {
  showToast('配置已保存并生成新版本');
};
</script>

<template>
  <section class="flex min-h-full animate-dm-page-fade flex-col bg-zinc-50/30 text-zinc-900">
    <header
      class="sticky top-0 z-10 flex h-16 box-border items-center justify-between border-b border-zinc-200 bg-white/90 px-6 py-4 backdrop-blur-sm"
    >
      <div>
        <h1 class="text-[15px] leading-5 font-semibold">配置中心</h1>
        <p class="mt-0.5 text-[11px] text-zinc-500">管理结构化字段、敏感规则与发布版本</p>
      </div>
      <DmButton variant="dark" @click="saveConfiguration"
        ><AppIcon name="save" />保存全局规则</DmButton
      >
    </header>

    <div class="mx-auto w-full max-w-[1200px] p-6 pb-12">
      <DmTabs v-model="activeTab" :items="CONFIG_TABS" label="配置类型" class="mb-6" />

      <div v-if="activeTab === 'schema'" class="grid grid-cols-[280px_minmax(0,1fr)] gap-5">
        <aside
          class="h-fit overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle"
        >
          <header
            class="flex items-center justify-between border-b border-zinc-200 bg-zinc-50/80 px-4 py-3"
          >
            <h2 class="text-[12px] font-semibold text-zinc-900">Schema 列表</h2>
            <DmButton variant="accent-ghost" icon-only aria-label="新建 Schema">
              <AppIcon name="plus" class="size-3.5" />
            </DmButton>
          </header>
          <div class="p-2">
            <DmInteractiveSurface
              v-for="schema in SCHEMA_LIST"
              :key="schema.id"
              type="button"
              :class="[
                'w-full rounded-md border p-3 text-left transition-colors',
                selectedSchemaId === schema.id
                  ? 'border-brand-200 bg-brand-50/70'
                  : 'border-transparent hover:bg-zinc-50',
              ]"
              @click="selectedSchemaId = schema.id"
            >
              <div class="flex items-start justify-between gap-2">
                <strong class="text-[12px] font-medium text-zinc-900">{{ schema.name }}</strong
                ><DmStatus
                  :label="schema.status"
                  :tone="schema.status === '已发布' ? 'success' : 'warning'"
                />
              </div>
              <div class="mt-2 flex items-center justify-between text-[10px] text-zinc-400">
                <span>{{ schema.version }}</span
                ><span>{{ schema.fieldCount }} 个字段</span>
              </div>
            </DmInteractiveSurface>
          </div>
        </aside>

        <section class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
          <header
            class="flex items-center justify-between border-b border-zinc-200 bg-zinc-50/80 px-5 py-3"
          >
            <div>
              <h2 class="text-[13px] font-semibold text-zinc-900">{{ selectedSchema?.name }}</h2>
              <p class="mt-0.5 text-[10px] text-zinc-400">
                字段配置 · 当前版本 {{ selectedSchema?.version }}
              </p>
            </div>
            <DmButton variant="secondary" @click="addField"
              ><AppIcon name="plus" />添加字段</DmButton
            >
          </header>
          <div
            class="grid grid-cols-[minmax(140px,1fr)_minmax(180px,1.4fr)_110px_80px_36px] items-center border-b border-zinc-200 bg-white px-4 py-2 text-[10px] font-semibold tracking-wider text-zinc-400 uppercase"
          >
            <span>字段名称</span><span>字段 Key</span><span>类型</span><span>必填</span
            ><span></span>
          </div>
          <div class="divide-y divide-zinc-100">
            <div
              v-for="field in fields"
              :key="field.id"
              class="grid grid-cols-[minmax(140px,1fr)_minmax(180px,1.4fr)_110px_80px_36px] items-center gap-2 px-4 py-2.5 hover:bg-zinc-50/70"
            >
              <DmInput
                v-model="field.name"
                appearance="unstyled"
                class="h-8 rounded border border-transparent bg-transparent px-2 text-[12px] font-medium text-zinc-900 outline-none hover:border-zinc-200 focus:border-brand-400 focus:bg-white"
              />
              <DmInput
                v-model="field.key"
                appearance="unstyled"
                class="h-8 rounded border border-transparent bg-transparent px-2 font-mono text-[11px] text-zinc-600 outline-none hover:border-zinc-200 focus:border-brand-400 focus:bg-white"
              />
              <DmSelect
                :id="`schema-field-type-${field.id}`"
                v-model="field.type"
                aria-label="字段类型"
                class="w-full"
              >
                <option>文本</option>
                <option>金额</option>
                <option>日期</option>
                <option>百分比</option>
              </DmSelect>
              <DmCheckbox
                :id="`schema-field-required-${field.id}`"
                v-model="field.required"
                label="是"
                class="items-center gap-1.5"
              />
              <DmButton
                variant="danger-ghost"
                icon-only
                aria-label="删除字段"
                @click="fields = fields.filter((item) => item.id !== field.id)"
              >
                <AppIcon name="close" class="size-3.5" />
              </DmButton>
            </div>
          </div>
          <footer
            class="flex items-center justify-between border-t border-zinc-200 bg-zinc-50/50 px-5 py-3"
          >
            <span class="text-[10px] text-zinc-400">保存后生成不可变版本，不影响历史抽取任务</span>
            <div class="flex gap-2">
              <DmButton variant="secondary" @click="showToast('草稿已保存')">保存草稿</DmButton
              ><DmButton @click="showToast('Schema 新版本已发布')">发布新版本</DmButton>
            </div>
          </footer>
        </section>
      </div>

      <div v-else class="mx-auto max-w-[900px] space-y-6">
        <section class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
          <header
            class="flex items-center gap-2.5 border-b border-zinc-200 bg-zinc-50/80 px-5 py-3"
          >
            <span
              class="flex size-6 items-center justify-center rounded border border-brand-100 bg-brand-50 text-brand-600"
              ><AppIcon name="brain-circuit" class="size-3.5"
            /></span>
            <h2 class="text-[13px] font-semibold text-zinc-900">AI 审核干预阈值</h2>
          </header>
          <div class="space-y-6 p-5">
            <p class="text-[12px] text-zinc-500">
              设定系统对 AI 抽取置信度的容忍策略。此配置直接影响自动归档率与人工审核工作量。
            </p>
            <label class="block"
              ><span class="mb-1 flex items-end justify-between"
                ><strong class="text-[13px] font-medium text-zinc-900"
                  >自动通过阈值 (Auto-Approve)</strong
                ><span
                  class="rounded border border-emerald-100 bg-emerald-50 px-1.5 py-0.5 font-mono text-[12px] font-semibold text-emerald-700"
                  >{{ autoApproveThreshold }}%</span
                ></span
              ><small class="mb-3 block text-[11px] text-zinc-500"
                >高于该阈值的字段将被标记为高确信度，可默认自动流转。</small
              ><DmRange
                id="auto-approve-threshold"
                v-model.number="autoApproveThreshold"
                :min="0"
                :max="100"
                tone="success"
            /></label>
            <label class="block border-t border-zinc-100 pt-4"
              ><span class="mb-1 flex items-end justify-between"
                ><strong class="text-[13px] font-medium text-zinc-900"
                  >人工复核阈值 (Human-Review)</strong
                ><span
                  class="rounded border border-amber-200 bg-amber-50 px-1.5 py-0.5 font-mono text-[12px] font-semibold text-amber-700"
                  >{{ humanReviewThreshold }}%</span
                ></span
              ><small class="mb-3 block text-[11px] text-zinc-500"
                >介于人工复核与自动通过阈值间的字段，强制要求人工介入确认。</small
              ><DmRange
                id="human-review-threshold"
                v-model.number="humanReviewThreshold"
                :min="0"
                :max="100"
                tone="warning"
            /></label>
          </div>
        </section>

        <section class="overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-subtle">
          <header
            class="flex items-center justify-between border-b border-zinc-200 bg-zinc-50/80 px-5 py-3"
          >
            <div class="flex items-center gap-2.5">
              <span
                class="flex size-6 items-center justify-center rounded border border-amber-100 bg-amber-50 text-amber-600"
                ><AppIcon name="shield-alert" class="size-3.5"
              /></span>
              <h2 class="text-[13px] font-semibold text-zinc-900">数据合规与敏感信息拦截</h2>
            </div>
            <DmButton
              variant="accent-ghost"
              @click="showToast('自定义实体编辑器待配置', { tone: 'info' })"
            >
              <AppIcon name="plus" class="size-3" />添加自定义实体
            </DmButton>
          </header>
          <div class="p-5">
            <p class="mb-4 text-[12px] text-zinc-500">
              开启后，DocMind 引擎将在所有文档处理管线中强制注入以下敏感实体探针。
            </p>
            <div class="space-y-3">
              <DmCheckbox
                v-for="rule in sensitiveRules"
                :key="rule.id"
                :id="`sensitive-rule-${rule.id}`"
                v-model="rule.enabled"
                :label="rule.name"
                :description="rule.description"
                class="group flex cursor-pointer items-start gap-3 rounded-md border border-zinc-200 p-3.5 shadow-subtle transition-colors hover:border-zinc-300 hover:bg-zinc-50"
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>
