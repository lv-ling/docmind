<script setup lang="ts">
import InlineNotice from '@/components/InlineNotice.vue';

import SchemaBuilder from './components/SchemaBuilder.vue';
import SensitiveRulesPanel from './components/SensitiveRulesPanel.vue';
import { useSchemaList } from './composables/useSchemaList.js';
import { createDefaultSensitiveRules } from './model/schema-form.js';

const {
  activeTab,
  schemas,
  sensitiveTemplates,
  schemaTemplateCount,
  isLoadingConfiguration,
  configurationError,
  configurationNotice,
  isSavingSchema,
  isSavingSensitiveTemplate,
  schemaName,
  schemaDescription,
  fields,
  sensitiveName,
  sensitiveDescription,
  addField,
  removeField,
  moveField,
  saveSchema,
  saveSensitiveTemplate,
} = useSchemaList();

const defaultSensitiveRules = createDefaultSensitiveRules();

const handleTabKeydown = (event: KeyboardEvent): void => {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
  event.preventDefault();
  activeTab.value = event.key === 'ArrowLeft' || event.key === 'Home' ? 'schemas' : 'sensitive';
  const targetId = activeTab.value === 'schemas' ? 'schema-tab' : 'sensitive-tab';
  document.querySelector<HTMLElement>(`#${targetId}`)?.focus();
};
</script>

<template>
  <section class="page-stack">
    <header class="page-heading">
      <div>
        <p class="eyebrow">DATA BLUEPRINT / W2</p>
        <h1>配置中心</h1>
        <p>字段 Schema 与敏感信息规则分别版本化，抽取任务始终绑定不可变快照。</p>
      </div>
      <span class="record-count">
        {{ schemas.length }} SCHEMAS · {{ sensitiveTemplates.length }} PII SETS
      </span>
    </header>

    <div class="tab-strip" role="tablist" aria-label="配置类型">
      <button
        id="schema-tab"
        type="button"
        :class="{ active: activeTab === 'schemas' }"
        role="tab"
        :aria-selected="activeTab === 'schemas'"
        aria-controls="schema-panel"
        :tabindex="activeTab === 'schemas' ? 0 : -1"
        @click="activeTab = 'schemas'"
        @keydown="handleTabKeydown"
      >
        字段配置
      </button>
      <button
        id="sensitive-tab"
        type="button"
        :class="{ active: activeTab === 'sensitive' }"
        role="tab"
        :aria-selected="activeTab === 'sensitive'"
        aria-controls="sensitive-panel"
        :tabindex="activeTab === 'sensitive' ? 0 : -1"
        @click="activeTab = 'sensitive'"
        @keydown="handleTabKeydown"
      >
        敏感规则
      </button>
    </div>

    <InlineNotice
      v-if="configurationError"
      tone="danger"
      title="操作未完成"
      :detail="configurationError"
    />
    <InlineNotice
      v-if="configurationNotice"
      tone="success"
      title="保存成功"
      :detail="configurationNotice"
    />

    <div
      v-if="activeTab === 'schemas'"
      id="schema-panel"
      class="config-layout"
      role="tabpanel"
      aria-labelledby="schema-tab"
    >
      <aside class="config-register">
        <div class="section-heading section-heading--bordered">
          <div>
            <span>01</span>
            <h2>已发布配置</h2>
          </div>
          <small>{{ schemaTemplateCount }} 个复用模板</small>
        </div>
        <p v-if="isLoadingConfiguration">正在读取…</p>
        <ol v-else-if="schemas.length" class="compact-register">
          <li v-for="schema in schemas" :key="schema.id">
            <strong>{{ schema.name }}</strong>
            <span>{{ schema.description || '无描述' }}</span>
            <small>{{ schema.current_version_id ? '已发布' : '草稿' }}</small>
          </li>
        </ol>
        <p v-else class="muted-copy">尚无字段配置。右侧可创建第一份。</p>
      </aside>

      <SchemaBuilder
        v-model:schema-name="schemaName"
        v-model:schema-description="schemaDescription"
        v-model:fields="fields"
        :is-saving="isSavingSchema"
        @submit="saveSchema"
        @add-field="addField"
        @remove-field="removeField"
        @move-field="moveField"
      />
    </div>

    <SensitiveRulesPanel
      v-else
      id="sensitive-panel"
      role="tabpanel"
      aria-labelledby="sensitive-tab"
      v-model:sensitive-name="sensitiveName"
      v-model:sensitive-description="sensitiveDescription"
      :templates="sensitiveTemplates"
      :rules="defaultSensitiveRules"
      :is-saving="isSavingSensitiveTemplate"
      @submit="saveSensitiveTemplate"
    />
  </section>
</template>

<style src="./styles.css"></style>
