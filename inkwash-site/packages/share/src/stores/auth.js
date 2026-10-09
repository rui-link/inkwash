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
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

import { login as loginApi, logout as logoutApi, getMe, getProfile, getPermissions } from '../api/auth.js';
import { refreshTokenOnce } from '../http/index.js';

const USER_INFO_KEY = 'inkwash-user-info';
const CACHE_TTL_MS = 5 * 60 * 1000;

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null);
  const roles = ref([]);
  const perms = ref([]);
  const isLoggedIn = ref(false);
  const userInfoLoaded = ref(false);
  const lastSyncedAt = ref(0);

  const isAdmin = computed(() => roles.value.includes('*') || perms.value.includes('*:*:*'));

  function persistUserInfo() {
    try {
      localStorage.setItem(
        USER_INFO_KEY,
        JSON.stringify({
          user: user.value,
          roles: roles.value,
          perms: perms.value,
          syncedAt: lastSyncedAt.value,
        }),
      );
    } catch {
      /* ignore */
    }
  }

  function clearCachedUserInfo() {
    localStorage.removeItem(USER_INFO_KEY);
  }

  function isCacheFresh() {
    return lastSyncedAt.value > 0 && Date.now() - lastSyncedAt.value < CACHE_TTL_MS;
  }

  let pendingUserInfo = null;

  async function loadUserInfo() {
    const profile = await getProfile();
    user.value = profile;
    roles.value = profile.roles || [];
    try {
      const permissionViews = await getPermissions();
      const permList = Array.isArray(permissionViews) ? permissionViews : [];
      perms.value = permList.map((p) => p.authority).filter(Boolean);
    } catch {
      perms.value = [];
    }
    userInfoLoaded.value = true;
    lastSyncedAt.value = Date.now();
    persistUserInfo();
    return { roles: roles.value, perms: perms.value };
  }

  function getUserInfo(force = false) {
    if (!force && userInfoLoaded.value && user.value && isCacheFresh()) {
      return Promise.resolve({ roles: roles.value, perms: perms.value });
    }
    if (!force && pendingUserInfo) {
      return pendingUserInfo;
    }
    pendingUserInfo = loadUserInfo().finally(() => {
      pendingUserInfo = null;
    });
    return pendingUserInfo;
  }

  /**
   * 探测当前登录态：调用 GET /api/auth/me（token 经 HttpOnly cookie 自动携带）。
   * 200 → 已登录；401 → 未登录并清理本地会话缓存。
   * 幂等：并发调用返回同一 Promise。
   */
  async function bootstrap() {
    if (bootstrapPromise) return bootstrapPromise;
    bootstrapPromise = doBootstrap().finally(() => {
      bootstrapPromise = null;
    });
    return bootstrapPromise;
  }

  let bootstrapPromise = null;

  async function doBootstrap() {
    try {
      await getMe(bootstrapProbe());
      isLoggedIn.value = true;
      restoreCachedUserInfo();
      return true;
    } catch (err) {
      if (err?.response?.status !== 401) {
        throw err;
      }
      // 探测 401：若 refresh cookie 仍有效（访问令牌过期）则静默轮换后恢复会话；
      // 否则视为匿名访客（前端无法读取 HttpOnly cookie，交给后端判定）。
      if (await silentRefresh()) {
        try {
          await getMe(bootstrapProbe());
          isLoggedIn.value = true;
          restoreCachedUserInfo();
          return true;
        } catch {
          /* 恢复失败按访客处理 */
        }
      }
      clearLocalSession();
      return false;
    }
  }

  /** 认证探测专用标记：401 时不触发拦截器刷新/登出跳转 */
  function bootstrapProbe() {
    return { __skipAuthRefresh: true };
  }

  /** 静默轮换令牌：与拦截器 401 刷新共用同一 single-flight 请求；失败（无会话或已过期）静默返回 false，不触发登出跳转 */
  async function silentRefresh() {
    try {
      await refreshTokenOnce({ silent: true });
      return true;
    } catch {
      return false;
    }
  }

  function restoreCachedUserInfo() {
    try {
      const raw = localStorage.getItem(USER_INFO_KEY);
      if (!raw) return;
      const cached = JSON.parse(raw);
      user.value = cached.user || null;
      roles.value = cached.roles || [];
      perms.value = cached.perms || [];
      lastSyncedAt.value = cached.syncedAt || 0;
      userInfoLoaded.value = cached.syncedAt > 0;
    } catch {
      /* ignore corrupt cache */
    }
  }

  async function login(data) {
    await loginApi(data);
    await bootstrap();
    userInfoLoaded.value = false;
    pendingUserInfo = null;
    getUserInfo().catch(() => {
      /* 用户信息加载失败不阻塞登录，交由调用方决定是否提示 */
    });
  }

  async function logout() {
    try {
      await logoutApi();
    } catch (e) {
      console.error('Logout API failed:', e);
    } finally {
      clearLocalSession();
      window.dispatchEvent(new CustomEvent('inkwash:session-ended'));
    }
  }

  /**
   * 清理内存会话状态与本地用户信息缓存（token 在 HttpOnly cookie 中，JS 不持有）。
   */
  function clearLocalSession() {
    isLoggedIn.value = false;
    user.value = null;
    roles.value = [];
    perms.value = [];
    userInfoLoaded.value = false;
    lastSyncedAt.value = 0;
    clearCachedUserInfo();
  }

  function resetToken() {
    clearLocalSession();
  }

  window.addEventListener('inkwash:session-ended', () => clearLocalSession());

  return {
    user,
    roles,
    perms,
    isLoggedIn,
    isAdmin,
    login,
    logout,
    getUserInfo,
    bootstrap,
    resetToken,
    clearLocalSession,
  };
});
