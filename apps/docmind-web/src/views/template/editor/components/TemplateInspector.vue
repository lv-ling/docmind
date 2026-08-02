<script setup lang="ts">
import type { ControlledDocument } from '@/editor';

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
      <select v-model="selectedNodeId">
        <option v-for="(block, index) in editableBlocks" :key="block.id" :value="block.id">
          {{ index + 1 }} · {{ block.text.slice(0, 24) || '空段落' }}
        </option>
      </select>
    </label>
    <template v-if="selectedBlock">
      <label class="inspector-field">
        文字内容
        <textarea
          :value="selectedBlock.text"
          rows="7"
          @input="emit('update-text', ($event.target as HTMLTextAreaElement).value)"
        ></textarea>
      </label>
      <div class="inspector-grid">
        <label>
          字号（pt）
          <input
            type="number"
            min="6"
            max="96"
            :value="paragraphFontSize"
            @change="emit('update-font-size', Number(($event.target as HTMLInputElement).value))"
          />
        </label>
        <label>
          对齐
          <select
            :value="paragraphAlignment"
            @change="emit('update-alignment', ($event.target as HTMLSelectElement).value)"
          >
            <option value="left">左对齐</option>
            <option value="center">居中</option>
            <option value="right">右对齐</option>
            <option value="justify">两端对齐</option>
          </select>
        </label>
      </div>
      <button class="format-toggle" type="button" @click="emit('toggle-bold')">切换粗体</button>
    </template>
    <fieldset>
      <legend>页边距（mm）</legend>
      <label v-for="side in ['top', 'right', 'bottom', 'left'] as const" :key="side">
        {{ MARGIN_LABELS[side] }}
        <input
          type="number"
          min="0"
          max="100"
          :value="draft?.page_layout.margins[side].value"
          @change="emit('update-margin', side, Number(($event.target as HTMLInputElement).value))"
        />
      </label>
    </fieldset>
  </aside>
</template>
