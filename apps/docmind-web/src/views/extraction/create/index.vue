<script setup lang="ts">
import type {
  SchemaVersionId,
  SensitiveRuleTemplateVersionId,
  SourceVersionId,
  WorkspaceId,
} from '@/contracts';
import { DmButton } from '@/ui';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { createExtraction } from '@/api/extractions.js';
import { listSchemas, listSensitiveRuleTemplates } from '@/api/schemas.js';
import { AppIcon, InlineNotice } from '@/components/index.js';
import { RouteName } from '@/router/constants.js';
import { getQueryString } from '@/router/query.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

const route = useRoute();
const router = useRouter();
const workspace = useWorkspaceStore();
const workspaceId = computed(() => workspace.selectedId as WorkspaceId);
const sourceVersionId = computed(
  () => getQueryString(route.query.sourceVersionId) as SourceVersionId | null,
);
const schemas = ref<Awaited<ReturnType<typeof listSchemas>>>([]);
const sensitiveTemplates = ref<Awaited<ReturnType<typeof listSensitiveRuleTemplates>>>([]);
const schemaVersionId = ref<SchemaVersionId | ''>('');
const ruleVersionId = ref<SensitiveRuleTemplateVersionId | ''>('');
const isLoadingConfiguration = ref(true);
const isCreatingExtraction = ref(false);
const extractionError = ref('');

const loadExtractionConfiguration = async (): Promise<void> => {
  if (sourceVersionId.value === null) {
    extractionError.value = '缺少原始文档版本，请从文档详情页发起抽取';
    isLoadingConfiguration.value = false;
    return;
  }
  try {
    [schemas.value, sensitiveTemplates.value] = await Promise.all([
      listSchemas(workspaceId.value),
      listSensitiveRuleTemplates(workspaceId.value),
    ]);
    schemaVersionId.value =
      schemas.value.find((item) => item.current_version_id !== null)?.current_version_id ?? '';
    ruleVersionId.value =
      sensitiveTemplates.value.find((item) => item.current_version_id !== null)
        ?.current_version_id ?? '';
  } catch (caught) {
    extractionError.value = caught instanceof Error ? caught.message : '抽取配置加载失败';
  } finally {
    isLoadingConfiguration.value = false;
  }
};

const handleCreateExtraction = async (): Promise<void> => {
  if (sourceVersionId.value === null || schemaVersionId.value === '') return;
  isCreatingExtraction.value = true;
  extractionError.value = '';
  try {
    const accepted = await createExtraction(sourceVersionId.value, {
      schema_version_id: schemaVersionId.value,
      sensitive_rule_template_version_id: ruleVersionId.value === '' ? null : ruleVersionId.value,
    });
    await router.replace({
      name: RouteName.ExtractionReview,
      query: { extractionId: accepted.extraction_id },
    });
  } catch (caught) {
    extractionError.value = caught instanceof Error ? caught.message : '抽取任务创建失败';
  } finally {
    isCreatingExtraction.value = false;
  }
};

onMounted(loadExtractionConfiguration);
</script>

<template>
  <section class="page-stack extraction-create-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">EXTRACTION ORDER / W3</p>
        <h1>发起字段抽取</h1>
        <p>任务会固定绑定原件版本、Schema 版本与敏感规则版本，便于审计和复现。</p>
      </div>
      <span class="record-count">ASYNC JOB</span>
    </header>
    <InlineNotice
      v-if="extractionError"
      tone="danger"
      title="暂时无法发起任务"
      :detail="extractionError"
    />
    <form
      v-if="!isLoadingConfiguration"
      class="extraction-order"
      @submit.prevent="handleCreateExtraction"
    >
      <div class="order-number" aria-hidden="true">01</div>
      <section>
        <p class="eyebrow">IMMUTABLE INPUT</p>
        <h2>原始文档版本</h2>
        <code>{{ sourceVersionId ?? '未选择' }}</code>
      </section>
      <section>
        <label for="schema-version">字段配置版本</label>
        <select id="schema-version" v-model="schemaVersionId" required>
          <option value="" disabled>请选择已发布 Schema</option>
          <option
            v-for="schema in schemas.filter((item) => item.current_version_id)"
            :key="schema.id"
            :value="schema.current_version_id ?? ''"
          >
            {{ schema.name }} · 当前发布版
          </option>
        </select>
        <small>模型只能返回配置中声明的字段，应用层会再次进行 JSON Schema 校验。</small>
      </section>
      <section>
        <label for="rule-version">敏感规则版本</label>
        <select id="rule-version" v-model="ruleVersionId">
          <option value="">仅使用系统内置规则</option>
          <option
            v-for="template in sensitiveTemplates.filter((item) => item.current_version_id)"
            :key="template.id"
            :value="template.current_version_id ?? ''"
          >
            {{ template.name }} · 当前发布版
          </option>
        </select>
        <small>敏感明文会在发送模型前替换为稳定占位符，返回后受控恢复。</small>
      </section>
      <div class="order-checks">
        <span>✓ 原件哈希复核</span><span>✓ 模型前 PII 扫描</span><span>✓ 模型输出二次扫描</span
        ><span>✓ 字段证据与置信度</span>
      </div>
      <footer>
        <DmButton type="button" variant="secondary" @click="router.back()">取消</DmButton>
        <DmButton type="submit" :disabled="schemaVersionId === ''" :loading="isCreatingExtraction"
          >创建异步任务 <AppIcon name="arrow"
        /></DmButton>
      </footer>
    </form>
    <div v-else class="document-loading">正在读取可用配置…</div>
  </section>
</template>

<style src="./styles.css"></style>
