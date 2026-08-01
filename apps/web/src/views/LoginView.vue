<script setup lang="ts">
import { DmButton, DmTextField } from '@docmind/ui';
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ApiClientError } from '../api/client.js';
import AppIcon from '../components/AppIcon.vue';
import { isSafeWorkspaceRedirect } from '../session.js';
import { useAuthStore } from '../stores/auth.js';
import { useWorkspaceStore } from '../stores/workspace.js';

const email = ref('admin@docmind.local');
const password = ref('DocMind123!');
const error = ref('');
const submitting = ref(false);
const auth = useAuthStore();
const workspace = useWorkspaceStore();
const router = useRouter();
const route = useRoute();
const sessionExpired = computed(() => route.query.reason === 'expired');

const submit = async (): Promise<void> => {
  error.value = '';
  submitting.value = true;
  try {
    await auth.login(email.value.trim(), password.value);
    await workspace.load();
    const requested = isSafeWorkspaceRedirect(route.query.redirect) ? route.query.redirect : null;
    const target =
      requested ?? (workspace.selectedId === null ? '/' : `/w/${workspace.selectedId}/sources`);
    await router.replace(target);
  } catch (caught) {
    error.value =
      caught instanceof ApiClientError && caught.status === 401
        ? '邮箱或密码不正确'
        : caught instanceof Error
          ? caught.message
          : '登录失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <main class="login-page">
    <section class="login-story" aria-labelledby="login-story-title">
      <div class="login-wordmark">
        <span class="brand-mark brand-mark--light"><i></i><i></i><i></i></span>DOCMIND
      </div>
      <div class="login-story-copy">
        <p class="eyebrow eyebrow--light">DOCUMENT INTELLIGENCE / 01</p>
        <h1 id="login-story-title">把复杂文档<br />变成可靠数据</h1>
        <p>从上传、敏感信息替换到结构化抽取与人工复核，每一次处理都有证据、状态和审计边界。</p>
      </div>
      <ol class="login-process" aria-label="处理流程">
        <li><strong>01</strong><span>原件解析</span></li>
        <li><strong>02</strong><span>隐私过滤</span></li>
        <li><strong>03</strong><span>字段复核</span></li>
      </ol>
    </section>

    <section class="login-form-panel" aria-labelledby="login-title">
      <form class="login-form" @submit.prevent="submit">
        <p class="eyebrow">SECURE ACCESS</p>
        <h2 id="login-title">进入文档工作台</h2>
        <p class="login-intro">使用团队账号登录。会话凭证仅保留在当前浏览器标签页。</p>
        <div v-if="sessionExpired" class="form-error" role="status">
          会话已过期，请重新登录。登录后将返回刚才的页面。
        </div>
        <div v-if="error" class="form-error" role="alert">{{ error }}</div>
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
        <DmButton type="submit" size="large" :loading="submitting" loading-label="正在验证账号">
          登录工作台 <AppIcon name="arrow" />
        </DmButton>
        <p class="login-local-hint">本地开发账号已预填；生产环境不会提供默认凭证。</p>
      </form>
      <footer>支持 DOC · DOCX · PDF <span>·</span> 九国敏感信息识别</footer>
    </section>
  </main>
</template>
