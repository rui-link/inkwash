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
import { useAuthStore, getUserMenus, abortAllRequests } from '@inkwash/share';
import { createRouter, createWebHashHistory } from 'vue-router';

import Layout from '@/layout/index.vue';
import { usePermissionStore, useSettingsStore } from '@/stores';
import NProgress from '@/utils/nprogress.js';

import { constantRoutes } from './static-routes.js';

const modules = import.meta.glob('@/views/**/**.vue');

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
});

const whiteList = ['/login', '/401', '/403', '/500'];
let setupPromise = null;
const dynamicRouteRemovers = new Set();

export function resetRouterState() {
  for (const remove of dynamicRouteRemovers) {
    remove();
  }
  dynamicRouteRemovers.clear();
  setupPromise = null;
}

export function buildRoutes(menuList) {
  function resolveComponent(component) {
    if (component === 'Layout') return Layout;
    const path = `/src/views/${component}.vue`;
    return modules[path] || (() => import('@/views/error/404.vue'));
  }

  function findFirstLeafPath(route) {
    if (!route.children || route.children.length === 0) return route.path;
    return `${route.path.replace(/\/$/, '')}/${findFirstLeafPath(route.children[0])}`;
  }

  function buildNestedTree(items, parentPath) {
    return items
      .filter((m) => m.visible !== false)
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
      .map((menu) => {
        const absPath = menu.path.replace(/^\//, '');
        const routePath = parentPath
          ? absPath.startsWith(parentPath + '/')
            ? absPath.slice(parentPath.length + 1)
            : absPath
          : absPath;
        const hasChildren = menu.children && menu.children.length > 0;
        const route = {
          path: parentPath ? routePath : `/${absPath}`,
          name: String(menu.id),
          component: hasChildren ? Layout : menu.component ? resolveComponent(menu.component) : Layout,
          meta: {
            title: menu.name || menu.title,
            icon: menu.icon,
            hidden: menu.visible === false,
            keepAlive: menu.keepAlive,
            authority: menu.authority,
            id: menu.id,
            noTab: menu.component === 'about/index',
          },
        };
        if (menu.children && menu.children.length > 0) {
          route.children = buildNestedTree(menu.children, absPath);
          const firstLeaf = findFirstLeafPath(route);
          if (firstLeaf) {
            route.redirect = firstLeaf;
          }
        }
        return route;
      })
      .filter((r) => r.path);
  }

  const nestedTree = buildNestedTree(menuList, '');

  return nestedTree;
}

export function buildHiddenRoutes() {
  return [
    {
      path: '/account',
      component: Layout,
      children: [
        {
          path: '',
          name: 'Account',
          component: () => import('@/views/account/index.vue'),
          meta: { title: 'profile', hidden: true },
        },
      ],
    },
    {
      path: '/cms/article/edit',
      component: Layout,
      children: [
        {
          path: '',
          name: 'ArticleEdit',
          component: () => import('@/views/content/article/edit.vue'),
          meta: { title: 'editArticle', hidden: true },
        },
      ],
    },
    {
      path: '/cms/article/review',
      component: Layout,
      children: [
        {
          path: '',
          name: 'ArticleReview',
          component: () => import('@/views/content/article/review.vue'),
          meta: { title: 'reviewArticle', hidden: true },
        },
      ],
    },
  ];
}

async function doSetupRoutes() {
  const authStore = useAuthStore();
  if (!authStore.isLoggedIn) return false;

  try {
    await authStore.getUserInfo();
    useSettingsStore().loadPreference();
    const menuList = await getUserMenus();
    const dynamicRoutes = buildRoutes(menuList);

    for (const route of dynamicRoutes) {
      const remove = router.addRoute(route);
      if (remove) dynamicRouteRemovers.add(remove);
    }

    for (const route of buildHiddenRoutes()) {
      const remove = router.addRoute(route);
      if (remove) dynamicRouteRemovers.add(remove);
    }

    const permissionStore = usePermissionStore();
    permissionStore.setRoutes(dynamicRoutes);

    return true;
  } catch {
    authStore.resetToken();
    resetRouterState();
    return false;
  }
}

function setupRoutes() {
  if (!setupPromise) {
    setupPromise = doSetupRoutes().then((ok) => {
      if (!ok) setupPromise = null;
      return ok;
    });
  }
  return setupPromise;
}

router.beforeEach(async (to) => {
  abortAllRequests();
  NProgress.start();

  const authStore = useAuthStore();

  if (!setupPromise) {
    const ok = await setupRoutes();
    if (!ok) {
      if (whiteList.includes(to.path)) {
        return true;
      }
      return `/login?redirect=${encodeURIComponent(to.fullPath)}`;
    }
    return { path: to.path, query: to.query, hash: to.hash, replace: true };
  }

  if (authStore.isLoggedIn) {
    if (to.path === '/login') {
      return { path: '/' };
    } else if (to.path === '/') {
      return { path: '/home', replace: true };
    } else if (to.meta?.authority && !authStore.isAdmin) {
      const authority = Array.isArray(to.meta.authority) ? to.meta.authority : [to.meta.authority];
      const hasAuthority = authority.some((p) => authStore.perms.includes(p));
      return hasAuthority ? true : '/403';
    }
    return true;
  } else {
    if (whiteList.includes(to.path)) {
      return true;
    }
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`;
  }
});

router.afterEach(() => {
  NProgress.done();
});

export default router;
