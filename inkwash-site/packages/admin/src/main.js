import { useAuthStore, useLicenseStore } from '@inkwash/share';
import { ElLoading } from 'element-plus';
import { createPinia } from 'pinia';
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
import { createApp } from 'vue';

import App from './App.vue';
import { setupDirectives } from './directives/index.js';
import i18n from './locale/index.js';
import router from './router/index.js';
import { setupSessionReset } from './utils/session.js';

import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import '@/assets/styles/index.scss';
import '@/assets/icons';

async function bootstrap() {
  const app = createApp(App);
  const pinia = createPinia();
  app.use(pinia);
  app.use(i18n);
  app.directive('loading', ElLoading.directive);
  setupDirectives(app);
  setupSessionReset();
  // 先完成登录态探测（HttpOnly cookie 探测 + 静默刷新）再安装带鉴权守卫的路由器，
  // 否则刷新时守卫会在登录态解析前读取到 stale 的 isLoggedIn=false 并跳回登录页。
  await useAuthStore().bootstrap();
  app.use(router);
  const licenseStore = useLicenseStore();
  licenseStore.fetchLicenseInfo();
  app.mount('#app');
}

bootstrap();
