<script setup lang="ts">
import type { SourceDocument } from '@/contracts';
import { DmButton, DmStatus } from '@/ui';

import AppIcon from '@/components/AppIcon.vue';

import { formatSourceDate, getSourceVersionLabel } from '../model/source-list.js';

defineProps<{
  source: SourceDocument | null;
}>();

const emit = defineEmits<{
  'open-source': [];
  'start-extraction': [];
}>();
</script>

<template>
  <aside v-if="source" class="source-task-panel" aria-label="当前文档任务">
    <header>
      <div>
        <p class="eyebrow">CURRENT DOCUMENT</p>
        <h2>文档信息</h2>
      </div>
      <DmStatus
        :label="source.current_version_id === null ? '待上传' : '已登记'"
        :tone="source.current_version_id === null ? 'warning' : 'success'"
      />
    </header>

    <section class="source-task-identity">
      <span class="file-glyph"><AppIcon name="document" /></span>
      <div>
        <strong>{{ source.name }}</strong
        ><small>不可变原件</small>
      </div>
    </section>

    <section class="source-task-section">
      <h3>原件信息</h3>
      <dl>
        <div>
          <dt>来源版本</dt>
          <dd>
            <code>{{ getSourceVersionLabel(source) }}</code>
          </dd>
        </div>
        <div>
          <dt>登记时间</dt>
          <dd>{{ formatSourceDate(source.created_at) }}</dd>
        </div>
        <div>
          <dt>更新时间</dt>
          <dd>{{ formatSourceDate(source.updated_at) }}</dd>
        </div>
      </dl>
    </section>

    <section class="source-task-section">
      <h3>处理进度</h3>
      <ol class="source-task-steps">
        <li class="is-complete"><span>01</span><strong>原件登记</strong></li>
        <li class="is-current"><span>02</span><strong>字段配置</strong></li>
        <li><span>03</span><strong>抽取复核</strong></li>
        <li><span>04</span><strong>完成</strong></li>
      </ol>
    </section>

    <section class="source-task-section source-task-config">
      <h3>版本与规则</h3>
      <div><span>字段配置版本</span><strong>发起抽取时选择</strong></div>
      <div><span>敏感规则版本</span><strong>发起抽取时选择</strong></div>
    </section>

    <footer>
      <p>任务将绑定当前不可变原件版本，后续配置变更不会影响已创建的任务。</p>
      <DmButton
        variant="secondary"
        :disabled="source.current_version_id === null"
        @click="emit('open-source')"
      >
        查看文档详情
      </DmButton>
      <DmButton :disabled="source.current_version_id === null" @click="emit('start-extraction')">
        发起抽取 <AppIcon name="arrow" />
      </DmButton>
    </footer>
  </aside>

  <aside v-else class="source-task-panel source-task-panel--empty" aria-label="当前文档任务">
    <strong>选择一份文档</strong><span>右侧将显示版本、处理状态与后续操作。</span>
  </aside>
</template>
