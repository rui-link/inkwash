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
  <div class="logo-container">
    <router-link class="wh-full" to="/" style="display: flex; padding-left: 15px; padding-top: 15px">
      <img src="/logo.svg" alt="logo" class="logo-img" />
      <span v-if="!collapse" class="logo-title">{{ siteStore.name }}</span>
    </router-link>
  </div>
</template>

<script setup>
import { useSiteStore } from '@inkwash/share';
import { onMounted } from 'vue';

const siteStore = useSiteStore();

defineProps({
  collapse: { type: Boolean, required: true },
});

onMounted(() => {
  siteStore.fetchSiteInfo();
});
</script>

<style lang="scss" scoped>
.logo-container {
  width: 100%;
  height: $navbar-height;
  background-color: var(--menubar-bg);
  overflow: hidden;
}

.logo-img {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.logo-title {
  flex-shrink: 0;
  margin-left: 10px;
  font-size: 16px;
  font-weight: bold;
  color: var(--menubar-logo-text, #ffffff);
  white-space: nowrap;
}

.layout-top {
  .logo-container {
    width: $menubar-width;
  }

  &.hideMenuBar {
    .logo-container {
      width: $menubar-collapsed-width;
    }
  }
}
</style>
