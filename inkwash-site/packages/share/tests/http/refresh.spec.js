import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import http from '../../src/http/index.js';
import { createTokenRefresher } from '../../src/http/refresh.js';

const authMock = vi.hoisted(() => ({
  refreshToken: vi.fn(() => Promise.resolve({ ok: true })),
}));
vi.doMock('../../src/api/auth.js', () => ({
  refreshToken: authMock.refreshToken,
}));

const originalAdapter = http.defaults.adapter;

afterEach(() => {
  http.defaults.adapter = originalAdapter;
});

function mkResponse(status, data, config) {
  return {
    status,
    statusText: String(status),
    headers: {},
    data,
    config,
    request: {},
  };
}

// 按请求 URL 分发响应：其它请求（/user 校验接口）依次消费 userTurns。
// 自定义 adapter 需自行对非 2xx 状态走 reject（axios 内置 adapter 内部才调用 settle）。
function installAdapter(userTurns) {
  http.defaults.adapter = async (config) => {
    const status = userTurns.length ? userTurns.shift() : 200;
    const response = mkResponse(status, { status }, config);
    if (status >= 400) {
      const error = new Error('Request failed with status code ' + status);
      error.response = response;
      error.config = config;
      error.isAxiosError = true;
      throw error;
    }
    return response;
  };
}

describe('createTokenRefresher', () => {
  function makeRefresher({ fail = false, onFailure = vi.fn() } = {}) {
    const refresh = vi.fn(() => {
      if (fail) return Promise.reject(new Error('refresh failed'));
      return Promise.resolve({ credential: 'new.access' });
    });
    const onSuccess = vi.fn();
    const refresher = createTokenRefresher({
      performRefresh: refresh,
      onRefreshSuccess: onSuccess,
      onRefreshFailure: onFailure,
    });
    return { refresher, refresh, onSuccess, onFailure };
  }

  it('notifies success when refresh succeeds', async () => {
    const { refresher, refresh, onSuccess } = makeRefresher();

    await refresher.refreshOnce();

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(onSuccess).toHaveBeenCalledWith({ credential: 'new.access' });
  });

  it('resolves concurrent callers with a single refresh request', async () => {
    const { refresher, refresh } = makeRefresher();

    const [a, b] = [refresher.refreshOnce(), refresher.refreshOnce()];
    await Promise.all([a, b]);

    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it('rejects queued callers and notifies failure when refresh fails', async () => {
    const { refresher, refresh, onFailure } = makeRefresher({ fail: true });

    const [a, b] = [refresher.refreshOnce(), refresher.refreshOnce()];
    await expect(a).rejects.toThrow('refresh failed');
    await expect(b).rejects.toThrow('refresh failed');

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(onFailure).toHaveBeenCalledTimes(1);
  });

  it('allows a fresh refresh attempt after a failure', async () => {
    const refresh = vi.fn().mockRejectedValueOnce(new Error('busy')).mockResolvedValueOnce({ credential: 'ok' });
    const refresher = createTokenRefresher({
      performRefresh: refresh,
      onRefreshSuccess: vi.fn(),
      onRefreshFailure: vi.fn(),
    });

    await expect(refresher.refreshOnce()).rejects.toThrow('busy');
    await refresher.refreshOnce();

    expect(refresh).toHaveBeenCalledTimes(2);
  });

  it('silent refresh failures skip the failure notification (anonymous bootstrap safety)', async () => {
    const { refresher, refresh, onFailure } = makeRefresher({ fail: true });

    await expect(refresher.refreshOnce({ silent: true })).rejects.toThrow('refresh failed');

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(onFailure).not.toHaveBeenCalled();

    // A non-silent attempt still notifies the failure handler.
    await expect(refresher.refreshOnce()).rejects.toThrow('refresh failed');
    expect(onFailure).toHaveBeenCalledTimes(1);
  });

  it('a silent refresh in flight serves later non-silent callers (single rotation)', async () => {
    const { refresher, refresh } = makeRefresher();
    const first = refresher.refreshOnce({ silent: true });
    const second = refresher.refreshOnce();
    await Promise.all([first, second]);

    expect(refresh).toHaveBeenCalledTimes(1);
  });
});

describe('401 响应拦截器', () => {
  beforeEach(() => {
    authMock.refreshToken.mockReset();
    localStorage.clear();
    window.location.hash = '#/login'; // forceLogout 时跳过页面跳转（jsdom 不支持导航）
  });

  it('刷新令牌并重放失败的请求', async () => {
    const userTurns = [401, 200];
    installAdapter(userTurns);

    const res = await http.get('/user');

    expect(res).toEqual({ status: 200 });
    expect(authMock.refreshToken).toHaveBeenCalledTimes(1);
    expect(userTurns).toHaveLength(0);
  });

  it('并发 401 请求只触发一次刷新，随后各自重放', async () => {
    const userTurns = [401, 401, 200, 200];
    installAdapter(userTurns);

    const [a, b] = [http.get('/user'), http.get('/user')];
    await expect(a).resolves.toEqual({ status: 200 });
    await expect(b).resolves.toEqual({ status: 200 });

    expect(authMock.refreshToken).toHaveBeenCalledTimes(1);
    expect(userTurns).toHaveLength(0);
  });

  it('重放后仍返回 401 时不再刷新并触发登出', async () => {
    const sessionEnded = vi.fn();
    window.addEventListener('inkwash:session-ended', sessionEnded);
    try {
      const userTurns = [401, 401];
      installAdapter(userTurns);

      await expect(http.get('/user')).rejects.toMatchObject({
        response: { status: 401 },
      });

      expect(authMock.refreshToken).toHaveBeenCalledTimes(1);
      expect(sessionEnded).toHaveBeenCalledTimes(1);
    } finally {
      window.removeEventListener('inkwash:session-ended', sessionEnded);
    }
  });

  it('跳过认证探测（bootstrap）401 时不刷新也不强制登出', async () => {
    const sessionEnded = vi.fn();
    window.addEventListener('inkwash:session-ended', sessionEnded);
    const originalLocation = window.location;
    const fakeLocation = { hash: '', pathname: '/', href: 'http://localhost/' };
    Object.defineProperty(window, 'location', {
      value: fakeLocation,
      configurable: true,
    });
    try {
      const userTurns = [401];
      installAdapter(userTurns);

      await expect(http.get('/auth/me', { __skipAuthRefresh: true })).rejects.toMatchObject({
        response: { status: 401 },
      });

      expect(authMock.refreshToken).not.toHaveBeenCalled();
      expect(sessionEnded).not.toHaveBeenCalled();
      expect(fakeLocation.href).toBe('http://localhost/');
      expect(userTurns).toHaveLength(0);
    } finally {
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        configurable: true,
      });
      window.removeEventListener('inkwash:session-ended', sessionEnded);
    }
  });

  it('history 模式（web）强制登出重定向到 SPA 登录路由 /auth/login', async () => {
    const originalLocation = window.location;
    const fakeLocation = {
      hash: '',
      pathname: '/articles',
      href: 'http://localhost/articles',
    };
    Object.defineProperty(window, 'location', {
      value: fakeLocation,
      configurable: true,
    });
    try {
      const userTurns = [401, 401];
      installAdapter(userTurns);

      await expect(http.get('/user')).rejects.toMatchObject({
        response: { status: 401 },
      });

      expect(fakeLocation.href).toBe(`/auth/login?redirect=${encodeURIComponent('/articles')}`);
    } finally {
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        configurable: true,
      });
    }
  });

  it('hash 模式（admin）强制登出重定向到 SPA 登录路由 /login', async () => {
    const originalLocation = window.location;
    const fakeLocation = {
      hash: '#/articles',
      pathname: '/',
      href: 'http://localhost/#/articles',
    };
    Object.defineProperty(window, 'location', {
      value: fakeLocation,
      configurable: true,
    });
    try {
      const userTurns = [401, 401];
      installAdapter(userTurns);

      await expect(http.get('/user')).rejects.toMatchObject({
        response: { status: 401 },
      });

      expect(fakeLocation.hash).toBe(`/login?redirect=${encodeURIComponent('/articles')}`);
    } finally {
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        configurable: true,
      });
    }
  });
});
