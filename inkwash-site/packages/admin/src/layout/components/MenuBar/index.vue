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
  <div :class="{ 'has-logo': showLogo }">
    <AppLogo v-if="showLogo" :collapse="appStore.menuStatus !== 'opened'" />
    <el-scrollbar>
      <MainMenu :menu-list="permissionStore.routes" base-path="" />
    </el-scrollbar>
  </div>
</template>

<script setup>
import { useAppStore } from '@inkwash/share';
import { computed } from 'vue';

import { useSettingsStore, usePermissionStore } from '@/stores';

import AppLogo from './components/AppLogo.vue';
import MainMenu from './components/MainMenu.vue';

const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();

const showLogo = computed(() => settingsStore.showLogo);
</script>

<style lang="scss" scoped>
.has-logo {
  .el-scrollbar {
    height: calc(100vh - $navbar-height);
  }
}
</style>
