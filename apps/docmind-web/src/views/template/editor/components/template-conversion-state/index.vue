<script setup lang="ts">
import { InlineNotice } from '@/components/index.js';

defineOptions({ name: 'TemplateConversionState' });

defineProps<{
  status: string;
  failureCode: string | null;
}>();
</script>

<template>
  <section class="template-conversion-state">
    <span class="conversion-engine" aria-hidden="true"><i></i><b>DOC</b></span>
    <p class="eyebrow">DETERMINISTIC CONVERSION</p>
    <h2 v-if="status !== 'failed'">正在生成可编辑模板</h2>
    <h2 v-else>模板转换未完成</h2>
    <ol>
      <li class="done">不可变原件校验</li>
      <li :class="{ done: status === 'retrying' }">生成 PDF 对照预览</li>
      <li>解析受控文档模型与资源</li>
      <li>白名单 HTML 与告警审查</li>
    </ol>
    <InlineNotice
      v-if="failureCode"
      tone="danger"
      title="转换任务失败"
      :detail="`失败代码：${failureCode}`"
    />
  </section>
</template>
