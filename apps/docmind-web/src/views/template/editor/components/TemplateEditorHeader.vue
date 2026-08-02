<script setup lang="ts">
import type { TemplateVersion } from '@/contracts';
import { DmButton } from '@/ui';

const changeSummary = defineModel<string>('changeSummary', { required: true });

defineProps<{
  templateName: string;
  conversionStatus: string;
  currentVersion: TemplateVersion | null;
  hasUnsavedChanges: boolean;
  isCurrentVersion: boolean;
  blockingWarningCount: number;
  isSavingVersion: boolean;
  isPublishingVersion: boolean;
}>();

const emit = defineEmits<{
  back: [];
  save: [];
  publish: [];
}>();
</script>

<template>
  <header class="template-editor-header">
    <div>
      <button class="back-link" type="button" @click="emit('back')">← 返回模板登记簿</button>
      <p class="eyebrow">TEMPLATE STUDIO / {{ conversionStatus }}</p>
      <h1>{{ templateName }}</h1>
    </div>
    <div v-if="currentVersion" class="template-header-actions">
      <label>变更说明<input v-model="changeSummary" maxlength="1000" /></label>
      <DmButton
        variant="secondary"
        :disabled="!hasUnsavedChanges || !isCurrentVersion || changeSummary.trim().length === 0"
        :loading="isSavingVersion"
        @click="emit('save')"
      >
        保存为新版本
      </DmButton>
      <DmButton
        :disabled="
          hasUnsavedChanges ||
          !isCurrentVersion ||
          blockingWarningCount > 0 ||
          currentVersion.status === 'published'
        "
        :loading="isPublishingVersion"
        @click="emit('publish')"
      >
        确认并发布
      </DmButton>
    </div>
  </header>
</template>
