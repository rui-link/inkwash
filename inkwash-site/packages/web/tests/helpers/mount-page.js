import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createPinia } from 'pinia';
import { vi } from 'vitest';
import { createI18n } from 'vue-i18n';
import { createRouter, createMemoryHistory } from 'vue-router';

const messages = {
  'zh-CN': {
    articleList: {
      title: '文章',
      desc: '浏览所有文章',
      descSearch: '搜索',
      noArticles: '暂无文章',
      noResults: '没有找到相关文章',
    },
  },
};

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages,
});

export function spyMessages() {
  return {
    success: vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined),
    error: vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined),
    warning: vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined),
  };
}

export function resolveConfirm(action = 'confirm') {
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue(action);
}

export function rejectConfirm(action = 'cancel') {
  vi.spyOn(ElMessageBox, 'confirm').mockRejectedValue(action);
}

export async function createTestRouter(path, routes = []) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/article/:id?', component: { template: '<div />' } },
      ...routes,
    ],
  });
  router.push(path);
  await router.isReady();
  return router;
}

export function mountPage(component, router) {
  return mount(component, {
    global: {
      plugins: [ElementPlus, router, createPinia(), i18n],
      stubs: { teleport: true },
    },
  });
}
