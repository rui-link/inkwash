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
  <el-drawer v-model="settingsVisible" size="420" :title="$t('settings.project')">
    <div class="settings-body">
      <div class="settings-section">
        <h3 class="section-title">{{ $t('settings.theme') }}</h3>
        <div class="theme-list">
          <div
            v-for="t in themes"
            :key="t.id"
            class="theme-card"
            :class="{ active: settingsStore.theme === t.id }"
            @click="settingsStore.changeTheme(t.id)">
            <div class="theme-preview" :style="getThemePreviewStyle(t)" />
            <div class="theme-text">
              <div class="theme-name">{{ t.name }}</div>
              <div class="theme-name-en">{{ t.nameEn }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="settings-section">
        <h3 class="section-title">{{ $t('settings.pageset') }}</h3>
        <div class="settings-item">
          <span class="item-label">{{ $t('settings.showTabs') }}</span>
          <el-switch v-model="settingsStore.showTabs" />
        </div>
        <div class="settings-item">
          <span class="item-label">{{ $t('settings.fixHeader') }}</span>
          <el-switch v-model="settingsStore.fixHeader" />
        </div>
        <div class="settings-item">
          <span class="item-label">{{ $t('settings.showLogo') }}</span>
          <el-switch v-model="settingsStore.showLogo" />
        </div>
        <div class="settings-item">
          <span class="item-label">{{ $t('settings.showFooter') }}</span>
          <el-switch v-model="settingsStore.showFooter" />
        </div>
      </div>

      <div class="settings-section">
        <h3 class="section-title">{{ $t('settings.navigation') }}</h3>
        <LayoutSelect v-model="settingsStore.layout" @update:model-value="changeLayout" />
      </div>

      <div class="settings-section">
        <h3 class="section-title">{{ $t('settings.locale') || '语言' }}</h3>
        <el-radio-group :model-value="settingsStore.locale" @change="handleLocaleChange">
          <el-radio-button value="zh-CN">中文</el-radio-button>
          <el-radio-button value="en">English</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="settings-footer">
      <el-button type="primary" size="small" @click="saveSettings">
        {{ $t('common.save') }}
      </el-button>
      <el-button type="danger" plain size="small" @click="resetSettings">
        {{ $t('common.reset') }}
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup>
import { useAppStore, THEMES, getErrorMessage } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

import defaultSettings from '@/config/settings';
import { LayoutEnum } from '@/enums';
import { useSettingsStore } from '@/stores';

import LayoutSelect from './components/LayoutSelect.vue';

const { t } = useI18n();
const appStore = useAppStore();
const settingsStore = useSettingsStore();

const themes = THEMES;

function getThemePreviewStyle(theme) {
  if (theme.id === 'ink-dark') {
    return { background: `linear-gradient(135deg, #15171a, #1d2024)` };
  }
  return {
    background: `linear-gradient(135deg, ${theme.primary}, ${theme.secondary})`,
  };
}

const settingsVisible = computed({
  get() {
    return settingsStore.settingsVisible;
  },
  set() {
    settingsStore.settingsVisible = false;
  },
});

function changeLayout(layout) {
  settingsStore.changeLayout(layout);
  if (layout === LayoutEnum.TOP) {
    appStore.openMenuBar();
  }
}

function handleLocaleChange(val) {
  settingsStore.changeLocale(val);
}

function saveSettings() {
  settingsStore
    .syncPreference()
    .then(() => ElMessage.success(t('common.success')))
    .catch((e) => ElMessage.error(getErrorMessage(e, t('common.operationFailed'))));
}

function resetSettings() {
  settingsStore.showTabs = defaultSettings.showTabs;
  settingsStore.fixHeader = defaultSettings.fixHeader;
  settingsStore.showLogo = defaultSettings.showLogo;
  settingsStore.showFooter = defaultSettings.showFooter;
  settingsStore.layout = defaultSettings.layout;
  settingsStore.changeLocale(defaultSettings.locale);
  settingsStore.changeTheme('sky-blue');
  settingsStore.syncPreference();
}
</script>

<style lang="scss" scoped>
.settings-body {
  padding-bottom: 60px;
}

.settings-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 12px;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
  line-height: 1;
}

.theme-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 0 0 10px;
}

.theme-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.active {
    border-color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
}

.theme-preview {
  width: 48px;
  height: 32px;
  border-radius: 6px;
  flex-shrink: 0;
}

.theme-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.theme-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.theme-name-en {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 8px 10px;

  &:hover {
    background: var(--el-fill-color-light);
    border-radius: 6px;
  }
}

.item-label {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.settings-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 12px 20px;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
