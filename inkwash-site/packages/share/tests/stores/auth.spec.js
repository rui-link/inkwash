import { createPinia, setActivePinia } from 'pinia';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { getMe, login, logout, refreshToken } from '../../src/api/auth.js';
import http from '../../src/http/index.js';
import { useAuthStore } from '../../src/stores/auth.js';

vi.mock('../../src/api/auth.js', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getMe: vi.fn(),
  getProfile: vi.fn(),
  getPermissions: vi.fn(),
  refreshToken: vi.fn(),
}));

function unauthorizedError() {
  return Object.assign(new Error('unauthorized'), {
    response: { status: 401 },
  });
}

const cachedUser = { username: 'u', nickname: 'U' };

function seedCache() {
  localStorage.setItem(
    'inkwash-user-info',
    JSON.stringify({
      user: cachedUser,
      roles: ['ROLE_ADMIN'],
      perms: ['*:*:*'],
      syncedAt: Date.now(),
    }),
  );
}

describe('useAuthStore bootstrap', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('marks the session active and restores cached user info when /auth/me succeeds', async () => {
    seedCache();
    getMe.mockResolvedValue({ userId: 1, identity: 'admin' });

    const store = useAuthStore();
    const ok = await store.bootstrap();

    expect(ok).toBe(true);
    expect(store.isLoggedIn).toBe(true);
    expect(store.user).toEqual(cachedUser);
    expect(store.roles).toEqual(['ROLE_ADMIN']);
    expect(store.perms).toEqual(['*:*:*']);
  });

  it('clears the local session when /auth/me returns 401', async () => {
    seedCache();
    getMe.mockRejectedValue(unauthorizedError());

    const store = useAuthStore();
    const ok = await store.bootstrap();

    expect(ok).toBe(false);
    expect(store.isLoggedIn).toBe(false);
    expect(store.user).toBeNull();
    expect(localStorage.getItem('inkwash-user-info')).toBeNull();
  });

  it('does not require a token refresh when /auth/me 401s as a guest', async () => {
    getMe.mockRejectedValue(unauthorizedError());
    refreshToken.mockRejectedValue(Object.assign(new Error('no session'), { response: { status: 400 } }));

    const store = useAuthStore();
    const ok = await store.bootstrap();

    expect(ok).toBe(false);
    expect(store.isLoggedIn).toBe(false);
    expect(getMe).toHaveBeenCalledTimes(1);
    expect(getMe).toHaveBeenCalledWith({ __skipAuthRefresh: true });
    expect(refreshToken).toHaveBeenCalledTimes(1);
    expect(refreshToken).toHaveBeenCalledWith(undefined, {
      __skipAuthRefresh: true,
    });
  });

  it('silently restores the session when the refresh cookie is valid but the access token expired', async () => {
    seedCache();
    getMe.mockRejectedValueOnce(unauthorizedError()).mockResolvedValueOnce({ userId: 1, identity: 'admin' });
    refreshToken.mockResolvedValue({});

    const store = useAuthStore();
    const ok = await store.bootstrap();

    expect(ok).toBe(true);
    expect(store.isLoggedIn).toBe(true);
    expect(refreshToken).toHaveBeenCalledTimes(1);
    expect(getMe).toHaveBeenCalledTimes(2);
  });

  it('deduplicates concurrent bootstrap calls', async () => {
    let resolveMe;
    getMe.mockReturnValue(
      new Promise((r) => {
        resolveMe = r;
      }),
    );

    const store = useAuthStore();
    const [a, b] = [store.bootstrap(), store.bootstrap()];
    resolveMe({ userId: 1 });
    await Promise.all([a, b]);

    expect(getMe).toHaveBeenCalledTimes(1);
    expect(store.isLoggedIn).toBe(true);
  });

  it('isLoggedIn stays false while bootstrap is in flight so guards must await it before deciding (admin F5 race)', async () => {
    seedCache();
    let resolveMe;
    getMe.mockReturnValue(
      new Promise((r) => {
        resolveMe = r;
      }),
    );

    const store = useAuthStore();
    const boot = store.bootstrap();

    // A router guard evaluated at this instant (installed before the host
    // awaited bootstrap, e.g. admin main.js on page refresh) sees a stale
    // isLoggedIn=false and redirects to /login, bouncing a live session back
    // to the login page. The contract: hosts must await bootstrap() before
    // installing their guarded router.
    expect(store.isLoggedIn).toBe(false);

    resolveMe({ userId: 1, identity: 'admin' });
    await boot;

    expect(store.isLoggedIn).toBe(true);
  });

  it('routes the bootstrap silent refresh and the interceptor 401 refresh through one single-flight request', async () => {
    const originalAdapter = http.defaults.adapter;
    getMe.mockRejectedValueOnce(unauthorizedError()).mockResolvedValue({ userId: 1, identity: 'admin' });

    // One shared, slow refresh promise: both channels must await the SAME in-flight call,
    // because the backend rotates refresh tokens per jti and treats a second rotation of
    // the old jti as reuse -> revokes the whole family -> forced logout on every F5.
    let resolveRefresh;
    const refreshPromise = new Promise((resolve) => {
      resolveRefresh = resolve;
    });
    refreshToken.mockReturnValue(refreshPromise);

    let userCalls = 0;
    http.defaults.adapter = async (config) => {
      const status = config.url.includes('/user') && ++userCalls === 1 ? 401 : 200;
      const response = { status, statusText: String(status), headers: {}, data: { status }, config, request: {} };
      if (status >= 400) {
        const error = new Error('Request failed with status code ' + status);
        error.response = response;
        error.config = config;
        error.isAxiosError = true;
        throw error;
      }
      return response;
    };

    try {
      const store = useAuthStore();
      const boot = store.bootstrap();
      const req = http.get('/user');
      await new Promise((r) => setTimeout(r, 0));
      resolveRefresh({});
      const [ok, res] = await Promise.all([boot, req]);

      expect(ok).toBe(true);
      expect(res).toEqual({ status: 200 });
      expect(refreshToken).toHaveBeenCalledTimes(1);
    } finally {
      http.defaults.adapter = originalAdapter;
    }
  });
});

describe('useAuthStore login/logout/session events', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('logins through the API then bootstraps the session', async () => {
    getMe.mockResolvedValue({ userId: 1, identity: 'admin' });
    login.mockResolvedValue({});

    const store = useAuthStore();
    await store.login({ identity: 'admin', plainPassword: 'x' });

    expect(login).toHaveBeenCalledTimes(1);
    expect(getMe).toHaveBeenCalledTimes(1);
    expect(store.isLoggedIn).toBe(true);
  });

  it('resets the in-memory session when the session ends', async () => {
    seedCache();
    getMe.mockResolvedValue({ userId: 1 });
    const store = useAuthStore();
    await store.bootstrap();
    expect(store.isLoggedIn).toBe(true);

    window.dispatchEvent(new CustomEvent('inkwash:session-ended'));

    expect(store.isLoggedIn).toBe(false);
    expect(store.user).toBeNull();
    expect(store.roles).toEqual([]);
    expect(store.perms).toEqual([]);
  });

  it('logouts through the API and clears the local session', async () => {
    const store = useAuthStore();
    store.isLoggedIn = true;
    logout.mockResolvedValue({});

    await store.logout();

    expect(logout).toHaveBeenCalledTimes(1);
    expect(store.isLoggedIn).toBe(false);
  });

  it('broadcasts the session-end event so host apps can drop dynamic routes', async () => {
    const store = useAuthStore();
    store.isLoggedIn = true;
    logout.mockResolvedValue({});
    const onEnded = vi.fn();
    window.addEventListener('inkwash:session-ended', onEnded);

    await store.logout();

    expect(onEnded).toHaveBeenCalledTimes(1);
    expect(store.isLoggedIn).toBe(false);
  });
});
