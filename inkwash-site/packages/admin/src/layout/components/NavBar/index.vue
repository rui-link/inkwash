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
  <div class="navbar-container" :class="layoutClass">
    <NavbarLeft />
    <NavbarRight />
  </div>
</template>

<script setup>
import { computed } from 'vue';

import { useSettingsStore } from '@/stores';

import NavbarLeft from './components/NavbarLeft.vue';
import NavbarRight from './components/NavbarRight.vue';

const settingsStore = useSettingsStore();

const layoutClass = computed(() => {
  return settingsStore.layout === 'top' ? 'layout-top' : '';
});
</script>

<style lang="scss" scoped>
.navbar-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $navbar-height;
  background-color: var(--navbar-bg);
  background-image:
    radial-gradient(
      ellipse 180px 60px at 92% 110%,
      color-mix(in srgb, var(--accent-color) 8%, transparent) 0%,
      transparent 65%
    ),
    radial-gradient(
      ellipse 200px 70px at 6% -15%,
      color-mix(in srgb, var(--primary-color) 5%, transparent) 0%,
      transparent 65%
    ),
    var(--bg-pattern);
  background-repeat: no-repeat, no-repeat, repeat-x;
  background-size:
    auto,
    auto,
    110px 50px;
  border-bottom: 1px solid var(--navbar-border);
}
</style>
