<script setup lang="ts">
import type { TemplateVersion } from '@/contracts';

import type { TemplateDiffChange } from '../model/template-document.js';

defineProps<{
  version: TemplateVersion;
  diffChanges: TemplateDiffChange[];
}>();

const emit = defineEmits<{
  'focus-node': [nodeId: string | null, page: number | null];
}>();
</script>

<template>
  <aside class="template-audit-drawer">
    <details open>
      <summary>
        转换告警 <strong>{{ version.warnings.length }}</strong>
      </summary>
      <button
        v-for="warning in version.warnings"
        :key="warning.id"
        type="button"
        :class="`warning-${warning.severity}`"
        @click="emit('focus-node', warning.source_node_id, warning.page_number)"
      >
        <span>{{ warning.code }}</span
        ><strong>{{ warning.message }}</strong>
        <small>
          {{ warning.page_number ? `P${warning.page_number}` : '全局' }} ·
          {{ warning.blocking ? '阻断发布' : '可接受回退' }}
        </small>
      </button>
      <p v-if="version.warnings.length === 0">未发现版式转换告警。</p>
    </details>
    <details>
      <summary>
        后端版本 Diff <strong>{{ diffChanges.length }}</strong>
      </summary>
      <ol>
        <li v-for="change in diffChanges.slice(0, 100)" :key="`${change.kind}-${change.path}`">
          <span>{{ change.kind }}</span
          ><code>{{ change.path }}</code>
        </li>
      </ol>
      <p v-if="diffChanges.length === 0">首版或本版本没有结构差异。</p>
    </details>
  </aside>
</template>
