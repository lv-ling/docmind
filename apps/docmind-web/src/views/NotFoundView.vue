<script setup lang="ts">
import { computed } from 'vue';

import { RouteName } from '../router/constants.js';
import { useAuthStore } from '../stores/auth.js';

const auth = useAuthStore();
const target = computed(() => ({
  name: auth.isAuthenticated ? RouteName.SourceList : RouteName.Login,
}));
</script>

<template>
  <main class="login-form-panel">
    <section class="login-form">
      <p class="eyebrow">404 / NOT FOUND</p>
      <h2>页面不存在</h2>
      <p class="login-intro">当前地址无效，页面可能已被移动或删除。</p>
      <RouterLink class="back-link" :to="target">返回 DocMind</RouterLink>
    </section>
  </main>
</template>
