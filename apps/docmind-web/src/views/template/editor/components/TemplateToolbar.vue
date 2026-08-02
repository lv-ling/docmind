<script setup lang="ts">
import type { NativeEditorSessionStatus } from '@/api/templates.js';
import { DmButton, DmRange } from '@/ui';

import AppIcon from '@/components/AppIcon.vue';

const zoomPercentage = defineModel<number>('zoomPercentage', { required: true });
const originalPage = defineModel<number>('originalPage', { required: true });

defineProps<{
  isEditMode: boolean;
  isNativeMode: boolean;
  isCurrentVersion: boolean;
  isLoadingNativeEditor: boolean;
  nativeStatus: NativeEditorSessionStatus | null;
  hasUnsavedChanges: boolean;
  pageCount: number;
}>();

const emit = defineEmits<{
  'show-preview': [];
  'show-edit': [];
  'start-native-editor': [];
}>();
</script>

<template>
  <section class="template-toolbar">
    <div class="mode-switch" aria-label="模板模式">
      <DmButton
        variant="ghost"
        :class="{ active: !isEditMode && !isNativeMode }"
        @click="emit('show-preview')"
      >
        预览
      </DmButton>
      <DmButton
        variant="ghost"
        :class="{ active: isEditMode && !isNativeMode }"
        :disabled="!isCurrentVersion"
        @click="emit('show-edit')"
      >
        微调
      </DmButton>
      <DmButton
        variant="ghost"
        :class="{ active: isNativeMode }"
        :disabled="!isCurrentVersion || isLoadingNativeEditor"
        @click="emit('start-native-editor')"
      >
        {{ isLoadingNativeEditor ? '正在启动…' : '原生编辑 POC' }}
      </DmButton>
    </div>
    <label v-if="!isNativeMode">
      缩放
      <DmRange v-model="zoomPercentage" :min="50" :max="150" :step="5" />
      <span>{{ zoomPercentage }}%</span>
    </label>
    <div class="page-navigator">
      <DmButton
        variant="ghost"
        icon-only
        aria-label="上一页"
        :disabled="originalPage <= 1"
        @click="originalPage--"
      >
        <AppIcon name="chevron-left" />
      </DmButton>
      <span>原件 P{{ originalPage }} / {{ pageCount }}</span>
      <DmButton
        variant="ghost"
        icon-only
        aria-label="下一页"
        :disabled="originalPage >= pageCount"
        @click="originalPage++"
      >
        <AppIcon name="chevron-right" />
      </DmButton>
    </div>
    <span v-if="isNativeMode" class="template-save-state native">
      {{ nativeStatus?.status ?? '正在建立编辑会话' }}
      <template v-if="nativeStatus?.saved_at">· 已收到保存回调</template>
    </span>
    <span v-else class="template-save-state" :class="{ dirty: hasUnsavedChanges }">
      {{ hasUnsavedChanges ? '有未保存修改' : '版本已固化' }}
    </span>
  </section>
</template>
