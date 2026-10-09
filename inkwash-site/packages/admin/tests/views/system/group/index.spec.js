import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus, { ElMessage } from 'element-plus';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import i18n from '@/locale/index';

const { groupApi, roleApi } = vi.hoisted(() => ({
  groupApi: {
    getGroupTree: vi.fn(),
    createGroup: vi.fn(),
    updateGroup: vi.fn(),
    deleteGroup: vi.fn(),
    assignRoles: vi.fn(),
  },
  roleApi: { getRoleList: vi.fn() },
}));

vi.mock('@inkwash/share', () => ({
  groupApi,
  roleApi,
  getErrorMessage: (e, fallback = '') => fallback,
  BaseStatus: { ENABLE: 1, DISABLE: 0 },
  BaseStatusMap: {
    1: { label: '启用', type: 'success' },
    0: { label: '禁用', type: 'info' },
  },
}));

import GroupPage from '@/views/system/group/index.vue';

function mountGroup() {
  return mount(GroupPage, {
    global: { plugins: [ElementPlus, i18n], directives: { hasPerm: {} } },
  });
}

beforeEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
  roleApi.getRoleList.mockResolvedValue({ list: [] });
});

describe('admin group page', () => {
  it('shows an error message when the group tree fails to load', async () => {
    groupApi.getGroupTree.mockRejectedValue(new Error('boom'));
    const errorSpy = vi.spyOn(ElMessage, 'error').mockImplementation(() => {});

    mountGroup();
    await flushPromises();

    expect(errorSpy).toHaveBeenCalledTimes(1);
    expect(groupApi.getGroupTree).toHaveBeenCalledTimes(1);
  });

  it('renders the group tree when the API succeeds', async () => {
    groupApi.getGroupTree.mockResolvedValue([{ id: 1, name: '研发部', status: 1, remark: '' }]);
    const wrapper = mountGroup();
    await flushPromises();

    expect(wrapper.text()).toContain('研发部');
    expect(groupApi.getGroupTree).toHaveBeenCalledTimes(1);
  });
});
