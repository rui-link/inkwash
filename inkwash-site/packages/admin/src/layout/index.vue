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
  <div class="wh-full" :class="classObj">
    <div
      v-if="classObj.mobile && classObj.openMenuBar"
      class="wh-full fixed-lt bg-opacity-30 z-999 bg-black"
      @click="appStore.closeMenuBar()"></div>

    <MenuBar class="menubar-container" />

    <div :class="{ hasTabview: showTabs }" class="main-container">
      <div :class="{ 'fixed-header': fixHeader }">
        <NavBar />
        <Tabview v-if="showTabs" />
      </div>
      <AppMain />
      <Settings v-if="defaultSettings.showSettings" />
      <LicenseDialog />
    </div>
    <Footer v-if="showFooter" />
  </div>
</template>

<script setup>
import { useAppStore } from '@inkwash/share';
import { useWindowSize } from '@vueuse/core';
import { computed, watchEffect } from 'vue';

import defaultSettings from '@/config/settings';
import { DeviceEnum } from '@/enums';
import { useSettingsStore } from '@/stores';

import AppMain from './components/AppMain/index.vue';
import Footer from './components/Footer/index.vue';
import LicenseDialog from './components/LicenseDialog/index.vue';
import MenuBar from './components/MenuBar/index.vue';
import NavBar from './components/NavBar/index.vue';
import Settings from './components/Settings/index.vue';
import Tabview from './components/Tabview/index.vue';

const appStore = useAppStore();
const settingsStore = useSettingsStore();

const fixHeader = computed(() => settingsStore.fixHeader);
const showTabs = computed(() => settingsStore.showTabs);
const showFooter = computed(() => settingsStore.showFooter);
const layout = computed(() => settingsStore.layout);

const classObj = computed(() => ({
  hideMenuBar: appStore.menuStatus !== 'opened',
  openMenuBar: appStore.menuStatus === 'opened',
  mobile: appStore.device === DeviceEnum.MOBILE,
  'layout-left': layout.value === 'left',
  'layout-top': layout.value === 'top',
}));

let resizeTimer = null;
const width = useWindowSize().width;
const WIDTH = 992;

watchEffect(() => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    if (width.value < WIDTH) {
      appStore.toggleDevice(DeviceEnum.MOBILE);
      appStore.closeMenuBar();
    } else {
      appStore.toggleDevice(DeviceEnum.DESKTOP);
      if (width.value >= 1200) {
        appStore.openMenuBar();
      } else {
        appStore.closeMenuBar();
      }
    }
  }, 100);
});
</script>

<style lang="scss" scoped>
.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - $menubar-width);
  transition: width 0.28s;
}

.menubar-container {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 999;
  width: $menubar-width;
  height: 100%;
  overflow: hidden;
  background-color: var(--menubar-bg);
  transition: width 0.28s;

  :deep(.el-menu) {
    border: none;
  }
}

.main-container {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-left: $menubar-width;
  transition: margin-left 0.28s;
}

.layout-top {
  .fixed-header {
    top: $navbar-height;
    width: 100%;
  }

  .menubar-container {
    z-index: 999;
    display: flex;
    width: 100% !important;
    height: $navbar-height;

    :deep(.el-scrollbar) {
      flex: 1;
      height: $navbar-height;
    }

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title),
    :deep(.el-menu--horizontal) {
      height: $navbar-height;
      line-height: $navbar-height;
    }

    :deep(.el-menu--collapse) {
      width: 100%;
    }
  }

  .main-container {
    min-height: calc(100vh - $navbar-height);
    padding-top: $navbar-height;
    margin-left: 0;
  }
}

.hideMenuBar {
  .fixed-header {
    left: $menubar-collapsed-width;
    width: calc(100% - $menubar-collapsed-width);
  }

  .main-container {
    margin-left: $menubar-collapsed-width;
  }

  &.layout-top {
    .fixed-header {
      left: 0;
      width: 100%;
    }

    .main-container {
      margin-left: 0;
    }
  }
}

.layout-left.hideMenuBar {
  .menubar-container {
    width: $menubar-collapsed-width !important;
  }

  .main-container {
    margin-left: $menubar-collapsed-width;
  }

  &.mobile {
    .menubar-container {
      pointer-events: none;
      transition-duration: 0.3s;
      transform: translate3d(-210px, 0, 0);
    }

    .main-container {
      margin-left: 0;
    }
  }
}

.mobile {
  .fixed-header {
    left: 0;
    width: 100%;
  }

  .main-container {
    margin-left: 0;
  }

  &.layout-top {
    .menubar-container {
      z-index: 999;
      display: flex;
      width: 100% !important;
      height: $navbar-height;

      :deep(.el-scrollbar) {
        flex: 1;
        min-width: 0;
        height: $navbar-height;
      }
    }

    .main-container {
      padding-top: $navbar-height;
      margin-left: 0;
      overflow: hidden;
    }

    --el-menu-item-height: $navbar-height;
  }
}

.wh-full {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
</style>
