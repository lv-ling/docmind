import type {
  CreateSchemaRequest,
  ExtractionSchema,
  SensitiveRuleTemplate,
  WorkspaceId,
} from '@/contracts';
import { computed, onMounted, ref, watch } from 'vue';

import {
  createSchema,
  createSensitiveRuleTemplate,
  listSchemaTemplates,
  listSchemas,
  listSensitiveRuleTemplates,
} from '@/api/schemas.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

import {
  createDefaultSensitiveRules,
  createEditableSchemaField,
  toSchemaFieldInput,
  type EditableSchemaField,
} from '../model/schema-form.js';

export const useSchemaList = () => {
  const workspace = useWorkspaceStore();
  const workspaceId = computed(() => workspace.selectedId as WorkspaceId);
  const activeTab = ref<'schemas' | 'sensitive'>('schemas');
  const schemas = ref<ExtractionSchema[]>([]);
  const sensitiveTemplates = ref<SensitiveRuleTemplate[]>([]);
  const schemaTemplateCount = ref(0);
  const isLoadingConfiguration = ref(true);
  const configurationError = ref('');
  const configurationNotice = ref('');
  const isSavingSchema = ref(false);
  const isSavingSensitiveTemplate = ref(false);
  const schemaName = ref('');
  const schemaDescription = ref('');
  const fields = ref<EditableSchemaField[]>([createEditableSchemaField(0)]);
  const sensitiveName = ref('九国通用敏感信息规则');
  const sensitiveDescription = ref(
    '覆盖中国、美国、日本、韩国、德国、法国、英国、澳大利亚、荷兰的常见身份与联系方式。',
  );

  const loadConfiguration = async (): Promise<void> => {
    isLoadingConfiguration.value = true;
    configurationError.value = '';
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
      configurationError.value = caught instanceof Error ? caught.message : '配置加载失败';
    } finally {
      isLoadingConfiguration.value = false;
    }
  };

  const addField = (): void => {
    fields.value.push(createEditableSchemaField(fields.value.length));
  };

  const removeField = (clientId: string): void => {
    if (fields.value.length > 1) {
      fields.value = fields.value.filter((field) => field.clientId !== clientId);
    }
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
    configurationError.value = '';
    configurationNotice.value = '';
    const keys = fields.value.map((field) => field.key.trim());
    if (schemaName.value.trim().length === 0) {
      configurationError.value = '请输入配置名称';
      return;
    }
    if (keys.some((key) => !/^[A-Za-z_][A-Za-z0-9_]*$/.test(key))) {
      configurationError.value = '字段 Key 只能使用字母、数字和下划线，且不能以数字开头';
      return;
    }
    if (new Set(keys).size !== keys.length) {
      configurationError.value = '字段 Key 不能重复';
      return;
    }

    isSavingSchema.value = true;
    try {
      const request: CreateSchemaRequest = {
        name: schemaName.value.trim(),
        description: schemaDescription.value.trim(),
        fields: fields.value.map(toSchemaFieldInput),
      };
      const created = await createSchema(workspaceId.value, request);
      configurationNotice.value = `字段配置“${created.schema.name}”已创建并发布 V1`;
      schemaName.value = '';
      schemaDescription.value = '';
      fields.value = [createEditableSchemaField(0)];
      await loadConfiguration();
    } catch (caught) {
      configurationError.value = caught instanceof Error ? caught.message : '字段配置保存失败';
    } finally {
      isSavingSchema.value = false;
    }
  };

  const saveSensitiveTemplate = async (): Promise<void> => {
    configurationError.value = '';
    configurationNotice.value = '';
    isSavingSensitiveTemplate.value = true;
    try {
      const created = await createSensitiveRuleTemplate(workspaceId.value, {
        name: sensitiveName.value.trim(),
        description: sensitiveDescription.value.trim(),
        rules: createDefaultSensitiveRules(),
      });
      configurationNotice.value = `敏感规则“${created.template.name}”已创建并发布 V1`;
      await loadConfiguration();
    } catch (caught) {
      configurationError.value = caught instanceof Error ? caught.message : '敏感规则模板保存失败';
    } finally {
      isSavingSensitiveTemplate.value = false;
    }
  };

  watch(workspaceId, () => void loadConfiguration());
  onMounted(loadConfiguration);

  return {
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
  };
};
