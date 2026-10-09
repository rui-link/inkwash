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
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <keep-alive :include="cachedViews">
        <component :is="Component" :key="route.path" />
      </keep-alive>
    </router-view>
  </section>
</template>

<script setup>
import { computed } from 'vue';

import { useTabviewStore } from '@/stores';

const tabviewStore = useTabviewStore();
const cachedViews = computed(() => tabviewStore.cachedViews);
</script>

<style lang="scss" scoped>
.app-main {
  position: relative;
  flex: 1;
  width: 100%;
  min-height: 0;
  overflow-y: auto;
  background-color: var(--page-bg);
}

.fixed-header + .app-main {
  padding-top: $navbar-height;
}

.hasTabview .fixed-header + .app-main {
  padding-top: $navbar-height + $tabview-height;
}

.layout-top {
  .fixed-header + .app-main {
    padding-top: 0;
  }

  .hasTabview .fixed-header + .app-main {
    padding-top: $tabview-height;
  }
}
</style>
