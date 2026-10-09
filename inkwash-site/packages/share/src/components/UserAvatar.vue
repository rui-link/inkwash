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
  <el-avatar :size="size" :src="src" class="user-avatar">
    <svg v-if="!src" class="default-avatar" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.87 0-8 1.92-8 5v1h16v-1c0-3.08-4.13-5-8-5z" />
    </svg>
    <span v-else>{{ initial }}</span>
  </el-avatar>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: 'U' },
  size: { type: Number, default: 40 },
});

const initial = computed(() => props.name?.[0]?.toUpperCase() || 'U');
</script>

<style scoped>
.user-avatar {
  background: var(--el-avatar-bg-color, var(--accent-color, #e8d8c3));
  color: var(--el-avatar-text-color, var(--accent-color-contrast, #4a382a));
  font-family: inherit;
  font-weight: 600;
  flex-shrink: 0;
}

.default-avatar {
  width: 58%;
  height: 58%;
  opacity: 0.85;
}
</style>
