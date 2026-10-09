import { useAuthStore, abortAllRequests } from '@inkwash/share';
import NProgress from 'nprogress';

import 'nprogress/nprogress.css';
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
import { createRouter, createWebHistory } from 'vue-router';

import AuthLayout from '@/layout/AuthLayout.vue';
import MainLayout from '@/layout/MainLayout.vue';

NProgress.configure({ showSpinner: false });

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'ArticleList',
        component: () => import('@/views/ArticleList.vue'),
        meta: { title: 'articleList' },
      },
      {
        path: 'article/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/ArticleDetail.vue'),
        meta: { title: 'articleDetail' },
      },
      {
        path: 'qr-login',
        name: 'QrLoginConfirm',
        component: () => import('@/views/QrLoginConfirm.vue'),
        meta: { title: 'qrLogin' },
      },
      {
        path: 'auth/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { title: 'login' },
      },
      {
        path: 'auth/register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: { title: 'register' },
      },
      {
        path: '/403',
        name: 'Forbidden',
        component: () => import('@/views/Forbidden.vue'),
        meta: { title: 'forbidden' },
      },
      {
        path: '/500',
        name: 'ServerError',
        component: () => import('@/views/ServerError.vue'),
        meta: { title: 'serverError' },
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/NotFound.vue'),
        meta: { title: 'notFound' },
      },
    ],
  },
  {
    path: '/user',
    component: AuthLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'articles',
        name: 'MyArticles',
        component: () => import('@/views/user/MyArticles.vue'),
        meta: { title: 'myArticles' },
      },
      {
        path: 'articles/new',
        name: 'ArticleNew',
        component: () => import('@/views/user/ArticleEditor.vue'),
        meta: { title: 'articleNew' },
      },
      {
        path: 'articles/:id/edit',
        name: 'ArticleEdit',
        component: () => import('@/views/user/ArticleEditor.vue'),
        props: true,
        meta: { title: 'articleEdit', sidebar: false },
      },
      {
        path: 'interactions',
        name: 'MyInteractions',
        component: () => import('@/views/user/MyInteractions.vue'),
        meta: { title: 'myInteractions' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: 'profile' },
      },
      {
        path: 'security',
        name: 'SecuritySettings',
        component: () => import('@/views/user/SecuritySettings.vue'),
        meta: { title: 'accountSecurity' },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  abortAllRequests();
  NProgress.start();
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }
});

router.afterEach(() => {
  NProgress.done();
});

router.onError((error) => {
  NProgress.done();
  console.error('Router error:', error);
});

export default router;
