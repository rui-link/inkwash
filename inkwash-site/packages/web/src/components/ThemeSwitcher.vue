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
  <el-dropdown @command="handleThemeCommand" trigger="click">
    <el-button class="theme-switcher-btn" :icon="Monitor" circle size="small" :aria-label="$t('layout.switchTheme')" />
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="t in themes"
          :key="t.id"
          :command="{ action: 'theme', theme: t }"
          class="theme-dropdown-item">
          <div class="theme-preview" :style="getThemePreviewStyle(t)" />
          <div class="theme-info">
            <span class="theme-name">{{ t.name }}</span>
            <span class="theme-name-en">{{ t.nameEn }}</span>
          </div>
          <el-icon v-if="appStore.theme === t.id" class="theme-active-icon"><Check /></el-icon>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { Monitor, Check } from '@element-plus/icons-vue';
import { useAppStore, THEMES } from '@inkwash/share';
import { computed } from 'vue';

const appStore = useAppStore();
const themes = THEMES;

function getThemePreviewStyle(theme) {
  if (theme.id === 'ink-dark') {
    return { background: `linear-gradient(135deg, #15171a, #1d2024)` };
  }
  return {
    background: `linear-gradient(135deg, ${theme.primary}, ${theme.secondary})`,
  };
}

function handleThemeCommand(cmd) {
  if (cmd.action === 'theme' && cmd.theme) {
    appStore.setTheme(cmd.theme.id);
  }
}
</script>

<style scoped>
.theme-switcher-btn {
  color: var(--text-color);
  transition: color 0.2s;
}

.theme-switcher-btn:hover {
  color: var(--accent-color);
}

.theme-dropdown-item {
  display: flex !important;
  align-items: center;
  gap: 12px;
  padding: 10px 12px !important;
}

.theme-preview {
  width: 36px;
  height: 22px;
  border-radius: 4px;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
}

.theme-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.theme-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.theme-name-en {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.theme-active-icon {
  color: var(--accent-color);
  font-size: 14px;
}
</style>
