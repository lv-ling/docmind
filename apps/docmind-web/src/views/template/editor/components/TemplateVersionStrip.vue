<script setup lang="ts">
import type { TemplateVersion, TemplateVersionId } from '@/contracts';
import { DmButton, DmInteractiveSurface } from '@/ui';

defineProps<{
  versions: TemplateVersion[];
  selectedVersionId: TemplateVersionId | null;
  isCurrentVersion: boolean;
  isRollingBack: boolean;
}>();

const emit = defineEmits<{
  'select-version': [version: TemplateVersion];
  rollback: [];
}>();
</script>

<template>
  <section class="template-version-strip" aria-label="模板版本历史">
    <div><span>版本历史</span><small>选择旧版本可查看后端 Diff 或恢复</small></div>
    <DmInteractiveSurface
      v-for="version in versions"
      :key="version.id"
      :class="{ active: version.id === selectedVersionId }"
      @click="emit('select-version', version)"
    >
      <strong>V{{ version.version_number }}</strong>
      <span>{{ version.status }}</span>
      <small>{{ new Date(version.created_at).toLocaleDateString('zh-CN') }}</small>
    </DmInteractiveSurface>
    <DmButton
      v-if="!isCurrentVersion"
      variant="secondary"
      size="small"
      :loading="isRollingBack"
      @click="emit('rollback')"
    >
      恢复此版本
    </DmButton>
  </section>
</template>
