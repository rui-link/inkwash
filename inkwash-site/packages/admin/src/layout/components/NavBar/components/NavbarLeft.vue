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
  <div class="navbar-left">
    <div class="navbar-switcher" @click="toggleMenuBar">
      <SvgIcon class="switcher" :class="{ 'is-active': appStore.menuStatus === 'opened' }" icon-class="side-toggle" />
    </div>
    <Breadcrumb />
  </div>
</template>

<script setup>
import { useAppStore } from '@inkwash/share';

import Breadcrumb from '@/components/Breadcrumb/index.vue';
import SvgIcon from '@/components/SvgIcon/index.vue';

const appStore = useAppStore();

function toggleMenuBar() {
  appStore.toggleMenuBar();
}
</script>

<style scoped lang="scss">
.navbar-left {
  display: flex;
  align-items: center;
  height: $navbar-height;
}

.navbar-switcher {
  display: flex;
  align-items: center;
  height: $navbar-height;
  padding: 0 15px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  flex-shrink: 0;
}

.switcher {
  vertical-align: middle;
  cursor: pointer;
  transform: scaleX(-1);
}

.switcher.is-active {
  transform: scaleX(1);
}
</style>
