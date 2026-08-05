<script setup lang="ts">
import type { SensitiveRuleInput, SensitiveRuleTemplate } from '@/contracts';
import { SENSITIVE_SUPPORTED_COUNTRY_CODES } from '@/contracts';
import { DmButton, DmTextField } from '@/ui';

import { InlineNotice } from '@/components/index.js';

defineOptions({ name: 'SensitiveRulesPanel' });

const sensitiveName = defineModel<string>('sensitiveName', { required: true });
const sensitiveDescription = defineModel<string>('sensitiveDescription', { required: true });

defineProps<{
  templates: SensitiveRuleTemplate[];
  rules: SensitiveRuleInput[];
  isSaving: boolean;
}>();

const emit = defineEmits<{
  submit: [];
}>();
</script>

<template>
  <div class="config-layout">
    <aside class="config-register">
      <div class="section-heading section-heading--bordered">
        <div>
          <span>01</span>
          <h2>已发布规则</h2>
        </div>
      </div>
      <ol v-if="templates.length" class="compact-register">
        <li v-for="item in templates" :key="item.id">
          <strong>{{ item.name }}</strong>
          <span>{{ item.description }}</span>
          <small>{{ item.current_version_id ? '已发布' : '草稿' }}</small>
        </li>
      </ol>
      <p v-else class="muted-copy">尚无敏感规则模板。</p>
    </aside>
    <form class="sensitive-builder" @submit.prevent="emit('submit')">
      <div class="section-heading section-heading--bordered">
        <div>
          <span>02</span>
          <h2>九国规则预设</h2>
        </div>
        <DmButton type="submit" size="small" :loading="isSaving">创建规则模板</DmButton>
      </div>
      <DmTextField id="sensitive-template-name" v-model="sensitiveName" label="规则名称" required />
      <DmTextField
        id="sensitive-template-description"
        v-model="sensitiveDescription"
        label="规则说明"
      />
      <div class="country-matrix" aria-label="覆盖国家">
        <span v-for="country in SENSITIVE_SUPPORTED_COUNTRY_CODES" :key="country">
          {{ country }}
        </span>
      </div>
      <div class="rule-blueprint">
        <article v-for="rule in rules" :key="rule.key">
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
</template>
