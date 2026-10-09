import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import i18n from '@/locale/index';

const baseRoutes = [
  { path: '/cms/article', component: { template: '<div />' } },
  { path: '/cms/article/edit', component: { template: '<div />' } },
  { path: '/cms/article/review', component: { template: '<div />' } },
  { path: '/cms/article/publish', component: { template: '<div />' } },
];

export async function createTestRouter(initialUrl = '/') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: baseRoutes,
  });
  await router.push(typeof initialUrl === 'string' ? initialUrl : initialUrl);
  return router;
}

export function mountPage(component, router) {
  return mount(component, {
    global: {
      plugins: [ElementPlus, router, i18n],
      directives: { hasPerm: {} },
    },
  });
}

export function spyMessages() {
  return {
    success: vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined),
    error: vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined),
    warning: vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined),
  };
}

export function resolveConfirm(value = 'confirm') {
  return vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue(value);
}

export function rejectConfirm(reason = 'cancel') {
  return vi.spyOn(ElMessageBox, 'confirm').mockRejectedValue(reason);
}
