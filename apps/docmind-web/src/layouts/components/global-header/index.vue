<script setup lang="ts">
import { DmButton } from '@/ui';

import { AppIcon } from '@/components/index.js';

defineOptions({ name: 'GlobalHeader' });

defineProps<{
  modelValue: string;
  hasNavigation: boolean;
  isMobileNavigationOpen: boolean;
  isNotificationPanelOpen: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'submit-search': [];
  'toggle-navigation': [];
  'toggle-notifications': [];
  'create-workflow': [];
}>();

const handleSearchInput = (event: Event): void => {
  emit('update:modelValue', (event.target as HTMLInputElement).value);
};
</script>

<template>
  <header class="workspace-header">
    <button
      v-if="hasNavigation"
      type="button"
      class="mobile-navigation-trigger"
      aria-label="切换导航菜单"
      :aria-expanded="isMobileNavigationOpen"
      aria-controls="workspace-navigation"
      @click="emit('toggle-navigation')"
    >
      <span></span><span></span><span></span>
    </button>

    <form class="global-search" role="search" @submit.prevent="emit('submit-search')">
      <button class="global-search__submit" type="submit" aria-label="执行全局搜索">
        <AppIcon name="search" />
      </button>
      <label class="dm-sr-only" for="global-search-input">全局搜索</label>
      <input
        id="global-search-input"
        :value="modelValue"
        type="search"
        placeholder="全局搜索：文档、提取结果、规则或实体..."
        @input="handleSearchInput"
      />
    </form>

    <div class="workspace-header__actions">
      <div class="notification-control">
        <button
          type="button"
          class="notification-trigger"
          aria-label="查看通知"
          :aria-expanded="isNotificationPanelOpen"
          @click="emit('toggle-notifications')"
        >
          <AppIcon name="bell" />
        </button>
        <div v-if="isNotificationPanelOpen" class="notification-panel" role="status">
          <strong>通知中心待接入</strong>
          <span>前端交互已经就绪，后续直接连接真实事件接口。</span>
        </div>
      </div>
      <span class="workspace-header__divider" aria-hidden="true"></span>
      <DmButton class="workspace-create-workflow" size="small" @click="emit('create-workflow')">
        <AppIcon name="plus" />新建工作流
      </DmButton>
    </div>
  </header>
</template>

<style scoped src="./styles.css"></style>
