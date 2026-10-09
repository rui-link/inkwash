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
  <el-dropdown class="locale-dropdown" trigger="click" @command="handleLocaleChange">
    <div class="locale-switcher">
      <SvgIcon icon-class="language" class="locale-globe" />
      <span class="locale-text">{{ localeText }}</span>
      <el-icon class="locale-icon"><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :class="{ 'is-active': currentLocale === 'zh-CN' }"> 中文 </el-dropdown-item>
        <el-dropdown-item command="en" :class="{ 'is-active': currentLocale === 'en' }"> English </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue';
import { useSettingsStore, i18n } from '@inkwash/share';
import { computed } from 'vue';

import SvgIcon from '@/components/SvgIcon/index.vue';

const settingsStore = useSettingsStore();

const currentLocale = computed(() => settingsStore.locale || 'zh-CN');

const localeText = computed(() => {
  return currentLocale.value === 'zh-CN' ? '中文' : 'EN';
});

function handleLocaleChange(locale) {
  settingsStore.changeLocale(locale);
  i18n.global.locale.value = locale;
}
</script>

<style scoped>
.locale-switcher {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.locale-switcher:hover {
  color: var(--accent-color);
  border-color: var(--accent-color);
}

.locale-globe {
  width: 15px;
  height: 15px;
}

.locale-icon {
  font-size: 12px;
}
</style>
