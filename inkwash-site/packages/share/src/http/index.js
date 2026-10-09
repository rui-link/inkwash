/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import axios from 'axios';

import { isLicenseError } from '../utils/error.js';
import { createTokenRefresher } from './refresh.js';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 50000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=utf-8',
    'X-Requested-With': 'XMLHttpRequest',
  },
});

// No automatic retry for any method, deliberately.
//
// axios-retry used to be installed here with a 2-retry policy for network errors and
// 5xx. Because its condition had no method guard, the non-idempotent
// `POST /cms/articles/{id}/commit` was replayed whenever the first response did not
// come back: the server had already flipped the article DRAFT -> PENDING, so the replay
// hit Article.submit()'s `status != DRAFT` guard and surfaced
// `error.article.submit_requires_draft` even though the submission had succeeded -- and
// the list stayed on "草稿" because handleCommit only refreshes after a success.
//
// Retrying 5xx is equally unsafe for POST: on 502/504 the transaction may already have
// committed, which @Transactional cannot undo. Removing the mechanism entirely keeps
// every request exactly-once from the client's point of view; a transient failure is
// reported to the user, who decides whether to try again. See
// packages/share/tests/http/retry.spec.js for the contract test.

let currentAbortController = null;
const activeAbortControllers = new Set();

export function abortAllRequests() {
  for (const controller of activeAbortControllers) {
    controller.abort();
  }
  activeAbortControllers.clear();
  currentAbortController = null;
}

export function createAbortSignal() {
  const controller = new AbortController();
  activeAbortControllers.add(controller);
  currentAbortController = controller;
  return controller.signal;
}

function redirectTo(path) {
  if (window.location.hash.startsWith('#/')) {
    window.location.hash = path;
  } else {
    window.location.href = path;
  }
}

function getCurrentPath() {
  if (window.location.hash.startsWith('#/')) {
    return window.location.hash.slice(1) || '/';
  }
  return window.location.pathname;
}

function getLoginPath() {
  return window.location.hash.startsWith('#/') ? '/login' : '/auth/login';
}

function forceLogout() {
  localStorage.removeItem('inkwash-user-info');
  window.dispatchEvent(new CustomEvent('inkwash:session-ended'));
  const currentPath = getCurrentPath();
  const loginPath = getLoginPath();
  if (currentPath !== loginPath && !currentPath.startsWith('/auth/')) {
    redirectTo(`${loginPath}?redirect=${encodeURIComponent(currentPath)}`);
  }
}

// HttpOnly cookie 对 JS 不可见：刷新是否可用由后端依据 secure_refresh cookie 判定。
// 刷新请求必须走同一个 single-flight 协调器：后端对 refresh token 按 jti 轮换，
// 同一旧 cookie 的第二次轮换会被判定为重用（reuse detection）并吊销整个 token 家族，
// 导致 F5 后必然强制登出。__skipAuthRefresh 让刷新 401 交由协调器的
// onRefreshFailure（silent 调用时不触发）处理，而不是在拦截器里直接 forceLogout。
const refresher = createTokenRefresher({
  performRefresh: async () => {
    const { refreshToken } = await import('../api/auth.js');
    return refreshToken(undefined, { __skipAuthRefresh: true });
  },
  onRefreshSuccess: () => {},
  onRefreshFailure: forceLogout,
});

export function refreshTokenOnce(options) {
  return refresher.refreshOnce(options);
}

http.interceptors.request.use(
  (config) => {
    const locale = localStorage.getItem('inkwash-locale') || 'zh-CN';
    config.headers['Accept-Language'] = locale;

    if (!config.signal && currentAbortController) {
      config.signal = currentAbortController.signal;
    }

    return config;
  },
  (error) => Promise.reject(error),
);

http.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (response.config?.responseType === 'arraybuffer') {
      return response;
    }
    return res;
  },
  (error) => {
    const { response } = error;
    if (response) {
      if (response.status === 401) {
        if (error.config?.__skipAuthRefresh) {
          // 认证探测（bootstrap）：不触发刷新，也不强制登出，匿名访客直接视为未登录。
          return Promise.reject(error);
        }
        const isRefreshCall = (error.config?.url || '').includes('/auth/token/refresh');
        // 令牌经 HttpOnly cookie 自动携带，刷新失败或重放仍 401 时直接登出，
        // 避免无谓的无限重放。
        if (!isRefreshCall && !error.config?._authRefreshRetried) {
          error.config._authRefreshRetried = true;
          return refresher
            .refreshOnce()
            .then(() => http(error.config))
            .catch(() => Promise.reject(error));
        }
        forceLogout();
        return Promise.reject(error);
      } else if (response.status === 403) {
        if (isLicenseError(response.data)) {
          window.dispatchEvent(new CustomEvent('inkwash:license-error', { detail: response.data }));
        } else {
          redirectTo('/403');
        }
      } else if (response.status >= 500) {
        redirectTo('/500');
      }
    }
    return Promise.reject(error);
  },
);

export default http;
