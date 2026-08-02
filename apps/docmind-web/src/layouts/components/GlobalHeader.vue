<script setup lang="ts">
import { DmButton, DmInput } from '@/ui';

import AppIcon from '@/components/AppIcon.vue';

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
</script>

<template>
  <header class="workspace-header">
    <DmButton
      v-if="hasNavigation"
      variant="ghost"
      icon-only
      class="mobile-navigation-trigger"
      aria-label="切换导航菜单"
      :aria-expanded="isMobileNavigationOpen"
      aria-controls="workspace-navigation"
      @click="emit('toggle-navigation')"
    >
      <AppIcon name="menu" />
    </DmButton>

    <form class="global-search" role="search" @submit.prevent="emit('submit-search')">
      <DmButton
        class="global-search__submit"
        type="submit"
        variant="ghost"
        icon-only
        aria-label="执行全局搜索"
      >
        <AppIcon name="search" />
      </DmButton>
      <label class="dm-sr-only" for="global-search-input">全局搜索</label>
      <DmInput
        id="global-search-input"
        :model-value="modelValue"
        appearance="unstyled"
        type="search"
        placeholder="全局搜索：文档、提取结果、规则或实体..."
        @update:model-value="emit('update:modelValue', String($event))"
      />
    </form>

    <div class="workspace-header__actions">
      <div class="notification-control">
        <DmButton
          class="notification-trigger"
          variant="ghost"
          icon-only
          aria-label="查看通知"
          :aria-expanded="isNotificationPanelOpen"
          @click="emit('toggle-notifications')"
        >
          <AppIcon name="bell" />
        </DmButton>
        <div v-if="isNotificationPanelOpen" class="notification-panel" role="status">
          <strong>通知中心待接入</strong>
          <span>前端交互已经就绪，后续直接连接真实事件接口。</span>
        </div>
      </div>
      <span class="workspace-header__divider" aria-hidden="true"></span>
      <DmButton variant="dark" size="small" @click="emit('create-workflow')">
        <AppIcon name="plus" />新建工作流
      </DmButton>
    </div>
  </header>
</template>

<style scoped src="./global-header.css"></style>
