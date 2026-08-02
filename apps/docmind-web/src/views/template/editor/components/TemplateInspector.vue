<script setup lang="ts">
import type { ControlledDocument } from '@/editor';
import { DmButton, DmInput, DmSelect, DmTextarea } from '@/ui';

import AppIcon from '@/components/AppIcon.vue';

import type { EditableDocumentBlock } from '../model/template-document.js';

const selectedNodeId = defineModel<string | null>('selectedNodeId', { required: true });

defineProps<{
  draft: ControlledDocument | null;
  editableBlocks: EditableDocumentBlock[];
  selectedBlock: EditableDocumentBlock | null;
  paragraphFontSize: number;
  paragraphAlignment: string;
}>();

const emit = defineEmits<{
  'update-text': [value: string];
  'update-font-size': [value: number];
  'update-alignment': [value: string];
  'toggle-bold': [];
  'update-margin': [side: 'top' | 'right' | 'bottom' | 'left', value: number];
}>();

const MARGIN_LABELS = { top: '上', right: '右', bottom: '下', left: '左' } as const;
</script>

<template>
  <aside class="template-inspector">
    <header><small>CONTROLLED NODES</small><strong>结构与微调</strong></header>
    <label class="inspector-field">
      编辑段落
      <DmSelect
        id="template-editor-block"
        :model-value="selectedNodeId ?? ''"
        @update:model-value="selectedNodeId = $event || null"
      >
        <option v-for="(block, index) in editableBlocks" :key="block.id" :value="block.id">
          {{ index + 1 }} · {{ block.text.slice(0, 24) || '空段落' }}
        </option>
      </DmSelect>
    </label>
    <template v-if="selectedBlock">
      <label class="inspector-field">
        文字内容
        <DmTextarea
          :model-value="selectedBlock.text"
          :rows="7"
          @update:model-value="emit('update-text', $event)"
        />
      </label>
      <div class="inspector-grid">
        <label>
          字号（pt）
          <DmInput
            type="number"
            :min="6"
            :max="96"
            :model-value="paragraphFontSize"
            @update:model-value="emit('update-font-size', Number($event))"
          />
        </label>
        <label>
          对齐
          <DmSelect
            id="template-editor-alignment"
            :model-value="paragraphAlignment"
            @update:model-value="emit('update-alignment', $event)"
          >
            <option value="left">左对齐</option>
            <option value="center">居中</option>
            <option value="right">右对齐</option>
            <option value="justify">两端对齐</option>
          </DmSelect>
        </label>
      </div>
      <DmButton class="format-toggle" variant="secondary" @click="emit('toggle-bold')">
        <AppIcon name="bold" />
        切换粗体
      </DmButton>
    </template>
    <fieldset>
      <legend>页边距（mm）</legend>
      <label v-for="side in ['top', 'right', 'bottom', 'left'] as const" :key="side">
        {{ MARGIN_LABELS[side] }}
        <DmInput
          type="number"
          :min="0"
          :max="100"
          :model-value="draft?.page_layout.margins[side].value ?? 0"
          @update:model-value="emit('update-margin', side, Number($event))"
        />
      </label>
    </fieldset>
  </aside>
</template>
