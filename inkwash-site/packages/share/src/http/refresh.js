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
/**
 * 令牌静默刷新协调器：保证同一时刻只有一个刷新请求在途，并发 401 请求排队等待，
 * 刷新成功后续跑（由调用方重放原请求），失败则统一拒绝并通知失败。
 * `{ silent: true }` 的调用不通知 onRefreshFailure（如匿名访客 bootstrap 探测）。
 */
export function createTokenRefresher({ performRefresh, onRefreshSuccess, onRefreshFailure }) {
  let isRefreshing = false;
  const queue = [];

  function flushQueue(error) {
    const pending = queue.splice(0);
    for (const p of pending) {
      if (error) p.reject(error);
      else p.resolve();
    }
  }

  function waitForRefresh() {
    return new Promise((resolve, reject) => {
      queue.push({ resolve, reject });
    });
  }

  function refreshOnce({ silent = false } = {}) {
    if (isRefreshing) {
      return waitForRefresh();
    }
    isRefreshing = true;
    return performRefresh()
      .then((res) => {
        onRefreshSuccess(res);
        flushQueue(null);
      })
      .catch((err) => {
        flushQueue(err);
        if (!silent) onRefreshFailure();
        throw err;
      })
      .finally(() => {
        isRefreshing = false;
      });
  }

  return { refreshOnce };
}
