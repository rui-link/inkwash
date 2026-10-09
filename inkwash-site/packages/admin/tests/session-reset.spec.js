import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import router from '@/router/index.js';
import { usePermissionStore } from '@/stores';
import { setupSessionReset } from '@/utils/session.js';

import { login, logout, getMe, getProfile, getPermissions } from '../../share/src/api/auth.js';
import { useAuthStore } from '../../share/src/stores/auth.js';

vi.mock('../../share/src/api/auth.js', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getMe: vi.fn(),
  getProfile: vi.fn(),
  getPermissions: vi.fn(),
}));

const { getUserMenus, getPreference } = vi.hoisted(() => ({
  getUserMenus: vi.fn(),
  getPreference: vi.fn(),
}));

vi.mock('@inkwash/share', async () => {
  const { useAuthStore } = await import('../../share/src/stores/auth.js');
  return {
    useAuthStore,
    getUserMenus,
    getPreference,
    abortAllRequests: vi.fn(),
    THEMES: [{ id: 'sky-blue', name: '天空蓝', primary: '#0070f3', secondary: '#07a' }],
  };
});

vi.mock('@/layout/index.vue', () => ({
  default: { name: 'Layout', template: '<div />' },
}));
vi.mock('@/utils/nprogress.js', () => ({
  default: { start: () => {}, done: () => {}, configure: () => {} },
}));

const menusA = [{ id: '20', name: 'miss', path: '/miss', component: 'error/404', sort: 1 }];

const menusB = [{ id: '30', name: 'miss', path: '/miss-b', component: 'error/404', sort: 1 }];

function stubSuccessfulSession() {
  login.mockResolvedValue({});
  logout.mockResolvedValue({});
  getMe.mockResolvedValue({ userId: 1 });
  getProfile.mockResolvedValue({ roles: ['ROLE_ADMIN'] });
  getPermissions.mockResolvedValue([]);
  getPreference.mockResolvedValue(null);
  getUserMenus.mockResolvedValue([]);
}

async function loginWithMenus(menus) {
  getUserMenus.mockResolvedValue(menus);
  const authStore = useAuthStore();
  await authStore.login({ identity: 'admin', plainPassword: 'password' });
  return authStore;
}

beforeEach(() => {
  setActivePinia(createPinia());
  localStorage.clear();
  vi.clearAllMocks();
  stubSuccessfulSession();
});

describe('admin session reset', () => {
  it('builds dynamic routes on login, then clears routes, menus and setup state on logout', async () => {
    setupSessionReset();
    const authStore = await loginWithMenus(menusA);
    await router.push('/miss');

    const permissionStore = usePermissionStore();
    expect(router.hasRoute('20')).toBe(true);
    expect(permissionStore.routes).toHaveLength(1);
    expect(permissionStore.routes[0].path).toBe('/miss');

    await authStore.logout();

    expect(router.hasRoute('20')).toBe(false);
    expect(router.hasRoute('Account')).toBe(false);
    expect(permissionStore.routes).toEqual([]);
    expect(authStore.isLoggedIn).toBe(false);
  });

  it('rebuilds fresh dynamic routes on the next login, without leaking the previous users routes', async () => {
    setupSessionReset();
    await loginWithMenus(menusA);
    await router.push('/miss');
    expect(router.hasRoute('20')).toBe(true);

    await useAuthStore().logout();
    expect(router.hasRoute('20')).toBe(false);

    await loginWithMenus(menusB);
    await router.push('/miss-b');

    expect(router.hasRoute('20')).toBe(false);
    expect(router.hasRoute('30')).toBe(true);
    expect(router.hasRoute('Account')).toBe(true);

    const permissionStore = usePermissionStore();
    expect(permissionStore.routes).toHaveLength(1);
    expect(permissionStore.routes[0].path).toBe('/miss-b');
  });
});
