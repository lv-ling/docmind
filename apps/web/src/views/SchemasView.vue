<script setup lang="ts">
import {
  FIELD_SENSITIVITY_LEVELS,
  SCHEMA_VALUE_TYPES,
  SENSITIVE_SUPPORTED_COUNTRY_CODES,
  type CreateSchemaRequest,
  type ExtractionSchema,
  type JsonValue,
  type SchemaFieldInput,
  type SchemaValueType,
  type SensitiveRuleInput,
  type SensitiveRuleTemplate,
  type WorkspaceId,
} from '@docmind/contracts';
import { DmButton, DmTextField } from '@docmind/ui';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import {
  createSchema,
  createSensitiveRuleTemplate,
  listSchemaTemplates,
  listSchemas,
  listSensitiveRuleTemplates,
} from '../api/schemas.js';
import InlineNotice from '../components/InlineNotice.vue';

interface EditableField {
  clientId: string;
  key: string;
  description: string;
  valueType: SchemaValueType;
  required: boolean;
  nullable: boolean;
  sensitivity: SchemaFieldInput['sensitivity'];
  defaultEnabled: boolean;
  defaultValue: string;
  extractionHint: string;
}

const createEditableField = (index: number): EditableField => ({
  clientId: crypto.randomUUID(),
  key: index === 0 ? 'document_title' : '',
  description: index === 0 ? '文档中出现的正式标题' : '',
  valueType: 'string',
  required: false,
  nullable: true,
  sensitivity: 'none',
  defaultEnabled: false,
  defaultValue: '',
  extractionHint: '',
});

const route = useRoute();
const workspaceId = computed(() => route.params.workspaceId as WorkspaceId);
const activeTab = ref<'schemas' | 'sensitive'>('schemas');
const schemas = ref<ExtractionSchema[]>([]);
const sensitiveTemplates = ref<SensitiveRuleTemplate[]>([]);
const schemaTemplateCount = ref(0);
const loading = ref(true);
const error = ref('');
const success = ref('');
const saving = ref(false);
const schemaName = ref('');
const schemaDescription = ref('');
const fields = ref<EditableField[]>([createEditableField(0)]);
const sensitiveName = ref('九国通用敏感信息规则');
const sensitiveDescription = ref(
  '覆盖中国、美国、日本、韩国、德国、法国、英国、澳大利亚、荷兰的常见身份与联系方式。',
);

const parseLiteral = (value: string, type: SchemaValueType): JsonValue => {
  if (type === 'number' || type === 'integer') {
    const number = Number(value);
    if (!Number.isFinite(number)) throw new Error('数字字段的默认值必须是有效数字');
    return number;
  }
  if (type === 'boolean') {
    if (!['true', 'false'].includes(value.toLowerCase()))
      throw new Error('布尔默认值只能是 true 或 false');
    return value.toLowerCase() === 'true';
  }
  if (type === 'object' || type === 'array') {
    const parsed = JSON.parse(value) as unknown;
    if (parsed === undefined) throw new Error('默认 JSON 无效');
    return parsed as JsonValue;
  }
  return value;
};

const toFieldInput = (field: EditableField, position: number): SchemaFieldInput => ({
  key: field.key.trim(),
  json_path: `$.${field.key.trim()}`,
  description: field.description.trim(),
  value_type: field.valueType,
  array_item_type: field.valueType === 'array' ? 'string' : null,
  required: field.required,
  nullable: field.nullable,
  default: field.defaultEnabled
    ? { kind: 'literal', value: parseLiteral(field.defaultValue, field.valueType) }
    : { kind: 'none' },
  sensitivity: field.sensitivity,
  constraints: {
    format: null,
    pattern: null,
    enum_values: [],
    min_length: null,
    max_length: null,
    minimum: null,
    maximum: null,
  },
  examples: [],
  extraction_hint: field.extractionHint.trim() || null,
  display: {
    mask: field.sensitivity === 'none' ? 'none' : field.sensitivity === 'high' ? 'full' : 'partial',
    view_role_keys: field.sensitivity === 'none' ? [] : ['owner', 'admin', 'reviewer'],
  },
  metadata: {},
  position,
});

const load = async (): Promise<void> => {
  loading.value = true;
  error.value = '';
  try {
    const [schemaItems, schemaTemplates, ruleTemplates] = await Promise.all([
      listSchemas(workspaceId.value),
      listSchemaTemplates(workspaceId.value),
      listSensitiveRuleTemplates(workspaceId.value),
    ]);
    schemas.value = schemaItems;
    schemaTemplateCount.value = schemaTemplates.length;
    sensitiveTemplates.value = ruleTemplates;
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '配置加载失败';
  } finally {
    loading.value = false;
  }
};

const addField = (): void => {
  fields.value.push(createEditableField(fields.value.length));
};
const removeField = (clientId: string): void => {
  if (fields.value.length > 1)
    fields.value = fields.value.filter((field) => field.clientId !== clientId);
};
const moveField = (index: number, direction: -1 | 1): void => {
  const target = index + direction;
  if (target < 0 || target >= fields.value.length) return;
  const copy = [...fields.value];
  const current = copy[index];
  const other = copy[target];
  if (current === undefined || other === undefined) return;
  copy[index] = other;
  copy[target] = current;
  fields.value = copy;
};

const saveSchema = async (): Promise<void> => {
  error.value = '';
  success.value = '';
  const keys = fields.value.map((field) => field.key.trim());
  if (schemaName.value.trim().length === 0) {
    error.value = '请输入配置名称';
    return;
  }
  if (keys.some((key) => !/^[A-Za-z_][A-Za-z0-9_]*$/.test(key))) {
    error.value = '字段 Key 只能使用字母、数字和下划线，且不能以数字开头';
    return;
  }
  if (new Set(keys).size !== keys.length) {
    error.value = '字段 Key 不能重复';
    return;
  }
  saving.value = true;
  try {
    const request: CreateSchemaRequest = {
      name: schemaName.value.trim(),
      description: schemaDescription.value.trim(),
      fields: fields.value.map(toFieldInput),
    };
    const created = await createSchema(workspaceId.value, request);
    success.value = `字段配置“${created.schema.name}”已创建并发布 V1`;
    schemaName.value = '';
    schemaDescription.value = '';
    fields.value = [createEditableField(0)];
    await load();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '字段配置保存失败';
  } finally {
    saving.value = false;
  }
};

const defaultSensitiveRules = (): SensitiveRuleInput[] => {
  const countries = [...SENSITIVE_SUPPORTED_COUNTRY_CODES];
  const base = (
    rule: Omit<
      SensitiveRuleInput,
      'country_codes' | 'locales' | 'confidence_threshold' | 'priority' | 'enabled'
    >,
    priority: number,
  ): SensitiveRuleInput => ({
    ...rule,
    country_codes: countries,
    locales: ['zh-CN', 'en-US', 'ja-JP', 'ko-KR', 'de-DE', 'fr-FR', 'en-GB', 'en-AU', 'nl-NL'],
    confidence_threshold: 0.75,
    priority,
    enabled: true,
  });
  return [
    base(
      {
        key: 'international_phone',
        name: '国际电话号码',
        description: '本地格式与 E.164 国际号码',
        data_type: 'phone_number',
        recognizer_kind: 'validator',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: 'e164_phone',
      },
      100,
    ),
    base(
      {
        key: 'email_address',
        name: '电子邮箱',
        description: '常见国际化邮箱地址',
        data_type: 'email_address',
        recognizer_kind: 'validator',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: 'email',
      },
      90,
    ),
    base(
      {
        key: 'identity_document',
        name: '身份号码',
        description: '九国身份证件号码与校验规则',
        data_type: 'identity_document',
        recognizer_kind: 'presidio',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: null,
      },
      80,
    ),
    base(
      {
        key: 'passport_document',
        name: '护照号码',
        description: '九国主流护照号码格式',
        data_type: 'passport',
        recognizer_kind: 'validator',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: 'passport_document',
      },
      70,
    ),
    base(
      {
        key: 'bank_account',
        name: '银行卡与银行账户',
        description: '银行卡、IBAN 与本地银行账户标识',
        data_type: 'bank_account',
        recognizer_kind: 'presidio',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: null,
      },
      60,
    ),
    base(
      {
        key: 'person_name',
        name: '人员姓名',
        description: '上下文中的自然人姓名',
        data_type: 'person_name',
        recognizer_kind: 'presidio',
        regex_pattern: null,
        regex_dialect: null,
        dictionary_terms: [],
        validator_name: null,
      },
      50,
    ),
  ];
};

const saveSensitiveTemplate = async (): Promise<void> => {
  error.value = '';
  success.value = '';
  saving.value = true;
  try {
    const created = await createSensitiveRuleTemplate(workspaceId.value, {
      name: sensitiveName.value.trim(),
      description: sensitiveDescription.value.trim(),
      rules: defaultSensitiveRules(),
    });
    success.value = `敏感规则“${created.template.name}”已创建并发布 V1`;
    await load();
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '敏感规则模板保存失败';
  } finally {
    saving.value = false;
  }
};

onMounted(load);
</script>

<template>
  <section class="page-stack">
    <header class="page-heading">
      <div>
        <p class="eyebrow">DATA BLUEPRINT / W2</p>
        <h1>配置中心</h1>
        <p>字段 Schema 与敏感信息规则分别版本化，抽取任务始终绑定不可变快照。</p>
      </div>
      <span class="record-count"
        >{{ schemas.length }} SCHEMAS · {{ sensitiveTemplates.length }} PII SETS</span
      >
    </header>
    <div class="tab-strip" role="tablist" aria-label="配置类型">
      <button
        :class="{ active: activeTab === 'schemas' }"
        role="tab"
        :aria-selected="activeTab === 'schemas'"
        @click="activeTab = 'schemas'"
      >
        字段配置
      </button>
      <button
        :class="{ active: activeTab === 'sensitive' }"
        role="tab"
        :aria-selected="activeTab === 'sensitive'"
        @click="activeTab = 'sensitive'"
      >
        敏感规则
      </button>
    </div>
    <InlineNotice v-if="error" tone="danger" title="操作未完成" :detail="error" />
    <InlineNotice v-if="success" tone="success" title="保存成功" :detail="success" />

    <div v-if="activeTab === 'schemas'" class="config-layout">
      <aside class="config-register">
        <div class="section-heading section-heading--bordered">
          <div>
            <span>01</span>
            <h2>已发布配置</h2>
          </div>
          <small>{{ schemaTemplateCount }} 个复用模板</small>
        </div>
        <p v-if="loading">正在读取…</p>
        <ol v-else-if="schemas.length" class="compact-register">
          <li v-for="schema in schemas" :key="schema.id">
            <strong>{{ schema.name }}</strong
            ><span>{{ schema.description || '无描述' }}</span
            ><small>{{ schema.current_version_id ? '已发布' : '草稿' }}</small>
          </li>
        </ol>
        <p v-else class="muted-copy">尚无字段配置。右侧可创建第一份。</p>
      </aside>
      <form class="schema-builder" @submit.prevent="saveSchema">
        <div class="section-heading section-heading--bordered">
          <div>
            <span>02</span>
            <h2>新建字段配置</h2>
          </div>
          <DmButton type="submit" size="small" :loading="saving">保存并发布 V1</DmButton>
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
          <article
            v-for="(field, index) in fields"
            :key="field.clientId"
            class="field-row"
            role="row"
          >
            <div class="field-position">
              <strong>{{ String(index + 1).padStart(2, '0') }}</strong
              ><button
                type="button"
                :disabled="index === 0"
                aria-label="上移字段"
                @click="moveField(index, -1)"
              >
                ↑</button
              ><button
                type="button"
                :disabled="index === fields.length - 1"
                aria-label="下移字段"
                @click="moveField(index, 1)"
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
                @click="removeField(field.clientId)"
              >
                删除字段
              </button>
            </div>
          </article>
        </div>
        <DmButton type="button" variant="secondary" @click="addField">＋ 添加字段</DmButton>
      </form>
    </div>

    <div v-else class="config-layout">
      <aside class="config-register">
        <div class="section-heading section-heading--bordered">
          <div>
            <span>01</span>
            <h2>已发布规则</h2>
          </div>
        </div>
        <ol v-if="sensitiveTemplates.length" class="compact-register">
          <li v-for="item in sensitiveTemplates" :key="item.id">
            <strong>{{ item.name }}</strong
            ><span>{{ item.description }}</span
            ><small>{{ item.current_version_id ? '已发布' : '草稿' }}</small>
          </li>
        </ol>
        <p v-else class="muted-copy">尚无敏感规则模板。</p>
      </aside>
      <form class="sensitive-builder" @submit.prevent="saveSensitiveTemplate">
        <div class="section-heading section-heading--bordered">
          <div>
            <span>02</span>
            <h2>九国规则预设</h2>
          </div>
          <DmButton type="submit" size="small" :loading="saving">创建规则模板</DmButton>
        </div>
        <DmTextField
          id="sensitive-template-name"
          v-model="sensitiveName"
          label="规则名称"
          required
        />
        <DmTextField
          id="sensitive-template-description"
          v-model="sensitiveDescription"
          label="规则说明"
        />
        <div class="country-matrix" aria-label="覆盖国家">
          <span v-for="country in SENSITIVE_SUPPORTED_COUNTRY_CODES" :key="country">{{
            country
          }}</span>
        </div>
        <div class="rule-blueprint">
          <article v-for="rule in defaultSensitiveRules()" :key="rule.key">
            <span>{{ rule.priority }}</span>
            <div>
              <strong>{{ rule.name }}</strong>
              <p>{{ rule.description }}</p>
            </div>
            <code>{{ rule.recognizer_kind }}</code>
          </article>
        </div>
        <InlineNotice
          tone="info"
          title="模型输出仍会进行二次 PII 扫描"
          detail="如果输出出现未登记的敏感明文，任务会阻断并进入人工复核，不会直接写入普通结果。"
        />
      </form>
    </div>
  </section>
</template>
