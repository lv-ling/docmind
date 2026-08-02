<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';

import { RouteName } from './router/constants.js';
import { SESSION_EXPIRED_EVENT, sessionExpiredLocation } from './session.js';
import { useWorkspaceStore } from './stores/workspace.js';

const route = useRoute();
const router = useRouter();
const workspace = useWorkspaceStore();

const handleSessionExpired = (): void => {
  if (route.name === RouteName.Login) return;
  workspace.reset();
  void router.replace(sessionExpiredLocation(route.fullPath));
};

onMounted(() => window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired));
onUnmounted(() => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired));
</script>

<template>
  <RouterView />
</template>
