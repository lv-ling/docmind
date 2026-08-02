<script setup lang="ts">
import { FIELD_SENSITIVITY_LEVELS, SCHEMA_VALUE_TYPES } from '@/contracts';
import { DmButton, DmTextField } from '@/ui';

import type { EditableSchemaField } from '../model/schema-form.js';

const schemaName = defineModel<string>('schemaName', { required: true });
const schemaDescription = defineModel<string>('schemaDescription', { required: true });
const fields = defineModel<EditableSchemaField[]>('fields', { required: true });

defineProps<{
  isSaving: boolean;
}>();

const emit = defineEmits<{
  submit: [];
  'add-field': [];
  'remove-field': [clientId: string];
  'move-field': [index: number, direction: -1 | 1];
}>();
</script>

<template>
  <form class="schema-builder" @submit.prevent="emit('submit')">
    <div class="section-heading section-heading--bordered">
      <div>
        <span>02</span>
        <h2>新建字段配置</h2>
      </div>
      <DmButton type="submit" size="small" :loading="isSaving">保存并发布 V1</DmButton>
    </div>
    <div class="schema-meta-grid">
      <DmTextField
        id="schema-name"
        v-model="schemaName"
        label="配置名称"
        required
        placeholder="例如：采购合同关键字段"
      />
      <DmTextField
        id="schema-description"
        v-model="schemaDescription"
        label="用途描述"
        placeholder="适用文档、抽取目标与复核说明"
      />
    </div>
    <div class="field-table" role="table" aria-label="字段定义">
      <div class="field-table-head" role="row">
        <span>#</span><span>字段定义</span><span>类型 / 敏感性</span><span>行为</span>
      </div>
      <article v-for="(field, index) in fields" :key="field.clientId" class="field-row" role="row">
        <div class="field-position">
          <strong>{{ String(index + 1).padStart(2, '0') }}</strong>
          <button
            type="button"
            :disabled="index === 0"
            aria-label="上移字段"
            @click="emit('move-field', index, -1)"
          >
            ↑
          </button>
          <button
            type="button"
            :disabled="index === fields.length - 1"
            aria-label="下移字段"
            @click="emit('move-field', index, 1)"
          >
            ↓
          </button>
        </div>
        <div class="field-primary">
          <label
            >字段 Key<input v-model="field.key" required placeholder="contract_number"
          /></label>
          <label
            >字段描述<textarea
              v-model="field.description"
              rows="2"
              placeholder="告诉模型这个字段是什么"
            ></textarea>
          </label>
          <label
            >抽取提示（可选）<input
              v-model="field.extractionHint"
              placeholder="例如：通常位于合同首页右上角"
          /></label>
        </div>
        <div class="field-classification">
          <label
            >数据类型<select v-model="field.valueType">
              <option v-for="type in SCHEMA_VALUE_TYPES" :key="type" :value="type">
                {{ type }}
              </option>
            </select></label
          >
          <label
            >敏感等级<select v-model="field.sensitivity">
              <option v-for="level in FIELD_SENSITIVITY_LEVELS" :key="level" :value="level">
                {{ level }}
              </option>
            </select></label
          >
          <label v-if="field.defaultEnabled"
            >默认值<input v-model="field.defaultValue" placeholder="找不到字段时使用"
          /></label>
        </div>
        <div class="field-behavior">
          <label><input v-model="field.required" type="checkbox" />必填</label>
          <label><input v-model="field.nullable" type="checkbox" />允许 null</label>
          <label><input v-model="field.defaultEnabled" type="checkbox" />设置默认值</label>
          <button
            type="button"
            class="danger-link"
            :disabled="fields.length === 1"
            @click="emit('remove-field', field.clientId)"
          >
            删除字段
          </button>
        </div>
      </article>
    </div>
    <DmButton type="button" variant="secondary" @click="emit('add-field')"> ＋ 添加字段 </DmButton>
  </form>
</template>
