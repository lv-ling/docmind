<script setup lang="ts">
import { DmButton, DmTextField } from '@/ui';
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ApiClientError } from '@/api/client.js';
import { AppIcon } from '@/components/index.js';
import { RouteName } from '@/router/constants.js';
import { isSafeAppRedirect } from '@/session.js';
import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';

const email = ref('admin@docmind.local');
const password = ref('DocMind123!');
const loginError = ref('');
const isSigningIn = ref(false);
const auth = useAuthStore();
const workspace = useWorkspaceStore();
const router = useRouter();
const route = useRoute();
const isSessionExpired = computed(() => route.query.reason === 'expired');

const handleSubmit = async (): Promise<void> => {
  loginError.value = '';
  isSigningIn.value = true;
  try {
    await auth.login(email.value.trim(), password.value);
    await workspace.load();
    const requested = isSafeAppRedirect(route.query.redirect) ? route.query.redirect : null;
    const target = requested ?? { name: RouteName.SourceList };
    await router.replace(target);
  } catch (caught) {
    loginError.value =
      caught instanceof ApiClientError && caught.status === 401
        ? '邮箱或密码不正确'
        : caught instanceof Error
          ? caught.message
          : '登录失败，请稍后重试';
  } finally {
    isSigningIn.value = false;
  }
};
</script>

<template>
  <main class="login-page">
    <section class="login-card" aria-labelledby="login-title">
      <header class="login-brand">
        <span class="login-brand__mark" aria-hidden="true"><AppIcon name="bot" /></span>
        <h1 id="login-title">DocMind 智能文档平台</h1>
        <p>围绕不可变原件的企业级文档处理与协作系统</p>
      </header>
      <form class="login-form" @submit.prevent="handleSubmit">
        <div v-if="isSessionExpired" class="form-error" role="status">
          会话已过期，请重新登录。登录后将返回刚才的页面。
        </div>
        <div v-if="loginError" class="form-error" role="alert">{{ loginError }}</div>
        <DmTextField
          id="login-email"
          v-model="email"
          label="邮箱"
          type="email"
          autocomplete="username"
          required
          placeholder="name@company.com"
        />
        <DmTextField
          id="login-password"
          v-model="password"
          label="密码"
          type="password"
          autocomplete="current-password"
          required
        />
        <DmButton type="submit" size="large" :loading="isSigningIn" loading-label="正在验证账号">
          登录工作台
        </DmButton>
      </form>
      <footer>
        <span>会话仅保留在当前标签页</span>
        <span aria-hidden="true">·</span>
        <span>支持 DOC、DOCX、PDF</span>
      </footer>
    </section>
  </main>
</template>

<style src="./styles.css"></style>
