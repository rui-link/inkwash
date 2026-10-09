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
  <footer class="layout-footer">
    <span>{{ siteStore.fullName }}</span>
    <span>{{ siteStore.meta?.copyright }}</span>
  </footer>
</template>

<script setup>
import { useSiteStore } from '@inkwash/share';
import { onMounted } from 'vue';

const siteStore = useSiteStore();

onMounted(() => {
  siteStore.fetchSiteInfo();
});
</script>

<style lang="scss" scoped>
.layout-footer {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  flex-wrap: wrap;
  text-align: center;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px 8px;
  font-size: 14px;
  color: var(--text-secondary);
  background-color: var(--footer-bg);
  border-top: 1px solid var(--border-color);
}
</style>
