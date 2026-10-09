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
  <el-menu
    :default-active="currentRoute.path"
    :collapse="appStore.menuStatus !== 'opened'"
    :background-color="'var(--menubar-bg)'"
    :text-color="'var(--menubar-text)'"
    :active-text-color="'var(--menubar-active-text)'"
    :unique-opened="false"
    :collapse-transition="false"
    :mode="layout === 'top' ? 'horizontal' : 'vertical'">
    <MenuItem
      v-for="route in permissionMenuList"
      :key="route.path"
      :item="route"
      :base-path="resolvePath(route.path)"
      :is-collapse="appStore.menuStatus !== 'opened'" />
  </el-menu>
</template>

<script setup>
import { useAppStore, useAuthStore } from '@inkwash/share';
import { computed } from 'vue';
import { useRoute } from 'vue-router';

import { useSettingsStore } from '@/stores';
import { handlePath } from '@/utils/index';

import MenuItem from './MenuItem.vue';

const appStore = useAppStore();
const authStore = useAuthStore();
const settingsStore = useSettingsStore();
const currentRoute = useRoute();
const layout = computed(() => settingsStore.layout);

const props = defineProps({
  menuList: { required: true, default: () => [], type: Array },
  basePath: { type: String, required: true },
});

const permissionMenuList = computed(() => {
  if (authStore.isAdmin) return props.menuList;
  return (props.menuList || []).filter((route) => {
    if (!route.meta?.authority) return true;
    const auths = Array.isArray(route.meta.authority) ? route.meta.authority : [route.meta.authority];
    return auths.some((p) => authStore.perms.includes(p));
  });
});

function resolvePath(routePath) {
  return handlePath(routePath, props.basePath);
}
</script>

<style scoped>
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: var(--menubar-hover-bg);
}
</style>
