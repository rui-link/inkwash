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
import { useAuthStore } from '../stores/auth.js';

/**
 * 权限判断工具。
 *
 * 注意：Pinia 的 store 必须在 setup() 或 onMounted 之后的已安装 Pinia 上下文中访问。
 * 若此函数在 setup 之外的异步回调/普通函数里被调用，主动抛出明确错误，避免静默失败。
 *
 * @returns {{ hasPerm: (p: string | string[]) => boolean, hasRole: (r: string | string[]) => boolean }}
 */
export function usePermission() {
  let authStore;
  try {
    authStore = useAuthStore();
  } catch (e) {
    throw new Error(
      'usePermission() must be called within a Vue setup() or a component that has Pinia initialized. Received: ' +
        e.message,
    );
  }

  function hasPerm(permission) {
    if (authStore.isAdmin) return true;
    if (typeof permission === 'string') {
      return authStore.perms.includes(permission);
    }
    if (Array.isArray(permission)) {
      return permission.some((p) => authStore.perms.includes(p));
    }
    return false;
  }

  function hasRole(role) {
    if (authStore.isAdmin) return true;
    if (typeof role === 'string') {
      return authStore.roles.includes(role);
    }
    if (Array.isArray(role)) {
      return role.some((r) => authStore.roles.includes(r));
    }
    return false;
  }

  return { hasPerm, hasRole };
}
