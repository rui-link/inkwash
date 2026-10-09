import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus, { ElMessage } from 'element-plus';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import i18n from '@/locale/index';

const { monitorApi } = vi.hoisted(() => ({
  monitorApi: {
    getLoginLogs: vi.fn(),
    deleteLoginLog: vi.fn(),
    batchDeleteLoginLogs: vi.fn(),
    clearLoginLogs: vi.fn(),
  },
}));

vi.mock('@inkwash/share', () => ({
  monitorApi,
  getErrorMessage: (e, fallback = '') => fallback,
  AuthTypeMap: {
    password: { label: '密码', type: 'info' },
  },
  formatDate: (d) => d,
}));

import LoginInfoPage from '@/views/monitor/login-info/index.vue';

function mountLoginInfo() {
  return mount(LoginInfoPage, {
    global: { plugins: [ElementPlus, i18n], directives: { hasPerm: {} } },
  });
}

beforeEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe('admin login-info page', () => {
  it('shows an error message when the log list fails to load', async () => {
    monitorApi.getLoginLogs.mockRejectedValue(new Error('boom'));
    const errorSpy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {});

    mountLoginInfo();
    await flushPromises();

    expect(errorSpy).toHaveBeenCalledTimes(1);
    expect(monitorApi.getLoginLogs).toHaveBeenCalledTimes(1);
  });

  it('renders login log rows when the API succeeds', async () => {
    monitorApi.getLoginLogs.mockResolvedValue({
      list: [
        {
          id: 1,
          identity: 'admin',
          loginType: 'password',
          address: '127.0.0.1',
          status: 1,
        },
      ],
      total: 1,
    });
    const wrapper = mountLoginInfo();
    await flushPromises();

    expect(wrapper.text()).toContain('admin');
    expect(wrapper.text()).toContain('127.0.0.1');
  });
});
