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
  <router-link to="/" class="logo" :style="textSize ? { fontSize: textSize } : undefined">
    <img :src="src" :alt="name" :width="width" :height="height" style="flex-shrink: 0" />
    <span class="logo-text">{{ name }}</span>
  </router-link>
</template>

<script setup>
defineProps({
  src: { type: String, default: '/favicon.ico' },
  name: { type: String, default: '' },
  width: { type: [Number, String], default: 28 },
  height: { type: [Number, String], default: 28 },
  textSize: { type: String, default: '' },
});
</script>

<style scoped>
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-text {
  font-family: var(--font-display);
  font-weight: 800;
  color: var(--text-color);
  letter-spacing: -0.03em;
}
</style>
