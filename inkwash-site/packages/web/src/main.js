import { useAuthStore, useAppStore, MdEditor, i18n, getErrorMessage } from '@inkwash/share';
import { ElLoading, ElMessage } from 'element-plus';
import { createPinia } from 'pinia';

import 'element-plus/dist/index.css';

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
import router from './router';

import './styles/global.scss';
import '@/assets/icons';
import '@/locale';

async function bootstrap() {
  const app = createApp(App);
  const pinia = createPinia();

  app.use(pinia);
  app.directive('loading', ElLoading.directive);
  app.config.errorHandler = (err, instance, info) => {
    console.error('[Global Error]', err, info);
    ElMessage.error(getErrorMessage(err, 'An unexpected error occurred'));
  };
  app.use(i18n);
  app.component('MdEditor', MdEditor);

  // 与 admin 一致的启动顺序：先完成登录态探测再安装带守卫的路由器，
  // 防止 /user 等受保护页面在刷新时被同步守卫误判跳回登录页。
  await useAuthStore().bootstrap();

  app.use(router);

  useAppStore().initTheme();

  app.mount('#app');
}

bootstrap();
