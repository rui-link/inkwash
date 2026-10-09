<!--
This file is part of Inkwash.
Copyright (C) 2026 ruilink team.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->
<template>
  <div class="navbar-right">
    <template v-if="!isMobile">
      <Notice id="header-notice" />
      <div class="setting-item" @click="toggle">
        <SvgIcon :icon-class="isFullscreen ? 'fullscreen-exit' : 'fullscreen'" />
      </div>
    </template>
    <el-dropdown class="setting-item" trigger="click" @command="changeLocale">
      <div class="setting-item-inner flex-center h-100% px-[12px] text-sm font-medium">
        <SvgIcon icon-class="language" class="locale-globe" />
        <span class="locale-text">{{ settingsStore.locale === 'zh-CN' ? '中' : 'EN' }}</span>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="zh-CN" :class="{ 'is-active': settingsStore.locale === 'zh-CN' }">
            中文
          </el-dropdown-item>
          <el-dropdown-item command="en" :class="{ 'is-active': settingsStore.locale === 'en' }">
            English
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    <el-dropdown
      class="setting-item"
      trigger="click"
      popper-class="account-dropdown-popper"
      placement="bottom-end"
      :popper-options="{
        modifiers: [{ name: 'offset', options: { offset: [5, 12] } }],
      }">
      <div class="setting-item-inner flex-center h-100% px-[10px]">
        <UserAvatar :src="authStore.user?.avatar" :name="authStore.user?.nickname" :size="24" />
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item disabled>
            <span class="font-bold">{{ authStore.user?.nickname || authStore.user?.username || 'User' }}</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click="toAccount">
            {{ $t('navbar.account') }}
          </el-dropdown-item>
          <el-dropdown-item v-if="defaultSettings.showSettings" @click="settingsStore.settingsVisible = true">
            {{ $t('settings.project') }}
          </el-dropdown-item>
          <el-dropdown-item divided @click="logout">
            {{ $t('navbar.logout') }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { useAppStore, useAuthStore, UserAvatar } from '@inkwash/share';
import { useFullscreen } from '@vueuse/core';
import { ElMessageBox } from 'element-plus';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import SvgIcon from '@/components/SvgIcon/index.vue';
import defaultSettings from '@/config/settings';
import { DeviceEnum } from '@/enums';
import { useSettingsStore } from '@/stores';

import Notice from './Notice/index.vue';

const { t } = useI18n();

const appStore = useAppStore();
const authStore = useAuthStore();
const settingsStore = useSettingsStore();

const route = useRoute();
const router = useRouter();
const { isFullscreen, toggle } = useFullscreen();
const isMobile = computed(() => appStore.device === DeviceEnum.MOBILE);

function toAccount() {
  router.push('/account');
}

async function logout() {
  try {
    await ElMessageBox.confirm(t('navbar.logoutConfirm'), t('common.notice'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
      lockScroll: false,
    });
  } catch {
    return;
  }
  await authStore.logout();
  router.push(`/login?redirect=${route.fullPath}`);
}

function changeLocale(locale) {
  settingsStore.changeLocale(locale);
}
</script>

<style lang="scss" scoped>
.navbar-right {
  display: flex;
  align-items: center;
  padding-right: 12px;
  gap: 4px;
}

.setting-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: $navbar-height;
  color: var(--el-text-color);
  cursor: pointer;

  &:hover {
    background: rgb(0 0 0 / 10%);
  }
}

.setting-item-inner {
  width: 100%;
  height: 100%;
}

.locale-globe {
  width: 15px;
  height: 15px;
  margin-right: 4px;
}

.layout-top {
  .setting-item,
  .el-icon {
    color: var(--el-color-white);
  }
}

.dark .setting-item:hover {
  background: rgb(255 255 255 / 20%);
}
</style>
