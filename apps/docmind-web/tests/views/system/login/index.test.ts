// @vitest-environment happy-dom

import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { defineComponent } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { RouteName } from '@/router/constants.js';
import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';
import LoginPage from '@/views/system/login/index.vue';

const WorkbenchStub = defineComponent({ template: '<div>工作台</div>' });

const createDeferred = () => {
  let resolve!: () => void;
  const promise = new Promise<void>((complete) => {
    resolve = complete;
  });
  return { promise, resolve };
};

const createLoginHarness = async () => {
  const pinia = createPinia();
  setActivePinia(pinia);
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: RouteName.Login, component: LoginPage },
      { path: '/workbench/source/list', name: RouteName.SourceList, component: WorkbenchStub },
    ],
  });
  await router.push({ name: RouteName.Login });
  await router.isReady();

  const wrapper = mount(LoginPage, { global: { plugins: [pinia, router] } });
  return { router, wrapper };
};

describe('login page', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    sessionStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('uses the real auth stores and enters the workspace after login', async () => {
    const loginRequest = createDeferred();
    const { router, wrapper } = await createLoginHarness();
    const auth = useAuthStore();
    const workspace = useWorkspaceStore();
    const loginSpy = vi.spyOn(auth, 'login').mockReturnValue(loginRequest.promise);
    const workspaceSpy = vi.spyOn(workspace, 'load').mockResolvedValue();

    expect(wrapper.text()).toContain('让复杂文档，变成可信数据。');
    expect(wrapper.text()).toContain('结构化智能抽取');
    expect(wrapper.text()).toContain('进入 DocMind 工作空间');
    expect(wrapper.text()).toContain('DocMind 企业版');
    expect(wrapper.text()).not.toContain('华东数据节点');

    await wrapper.get('#login-account').setValue('reviewer@company.com');
    await wrapper.get('#login-password').setValue('correct-password');
    await wrapper.get('form').trigger('submit');

    expect(wrapper.get('button[type="submit"]').text()).toContain('验证中...');
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined();
    expect(loginSpy).toHaveBeenCalledWith('reviewer@company.com', 'correct-password');

    loginRequest.resolve();
    await flushPromises();

    expect(workspaceSpy).toHaveBeenCalledOnce();
    expect(wrapper.get('main').classes()).toContain('opacity-0');

    await vi.advanceTimersByTimeAsync(700);
    await flushPromises();

    expect(router.currentRoute.value.name).toBe(RouteName.SourceList);
    wrapper.unmount();
  });

  it('prevents repeated submissions while verification is running', async () => {
    const loginRequest = createDeferred();
    const { wrapper } = await createLoginHarness();
    const loginSpy = vi.spyOn(useAuthStore(), 'login').mockReturnValue(loginRequest.promise);

    await wrapper.get('#login-account').setValue('reviewer@company.com');
    await wrapper.get('#login-password').setValue('correct-password');
    await wrapper.get('form').trigger('submit');
    await wrapper.get('form').trigger('submit');

    expect(loginSpy).toHaveBeenCalledOnce();
    wrapper.unmount();
  });

  it('shows and hides the password without changing its value', async () => {
    const { wrapper } = await createLoginHarness();
    const passwordInput = wrapper.get<HTMLInputElement>('#login-password');

    await passwordInput.setValue('correct-password');
    expect(passwordInput.attributes('type')).toBe('password');

    await wrapper.get('button[aria-label="显示密码"]').trigger('click');
    expect(passwordInput.attributes('type')).toBe('text');
    expect(passwordInput.element.value).toBe('correct-password');

    await wrapper.get('button[aria-label="隐藏密码"]').trigger('click');
    expect(passwordInput.attributes('type')).toBe('password');
    expect(passwordInput.element.value).toBe('correct-password');
    wrapper.unmount();
  });
});
