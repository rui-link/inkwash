import { THEMES, getPreference, savePreference } from '@inkwash/share';
import { useStorage } from '@vueuse/core';
/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import { defineStore } from 'pinia';
import { ref, watch } from 'vue';

import defaultSettings from '@/config/settings';
import i18n from '@/locale/index.js';
import { setStyleProperty } from '@/utils';
import { genMixColor } from '@/utils/color';

function setSurfaceVars(themeId) {
  const root = document.documentElement;
  const theme = findTheme(themeId);
  const mix = genMixColor(theme.primary);
  const isDark = themeId === 'ink-dark';

  if (isDark) {
    root.classList.add('dark');
    setStyleProperty('--menubar-bg', '#1d2024');
    setStyleProperty('--menubar-hover-bg', '#2e3238');
    setStyleProperty('--menubar-text', '#a3abb4');
    setStyleProperty('--menubar-logo-text', '#ffffff');
    setStyleProperty('--menubar-active-text', mix.DEFAULT);
    setStyleProperty('--navbar-bg', '#1d2024');
    setStyleProperty('--navbar-border', '#2e3238');
    setStyleProperty('--page-bg', '#15171a');
    setStyleProperty('--footer-bg', '#1d2024');
    setStyleProperty('--card-bg', '#1d2024');
    setStyleProperty('--card-header-bg', '#1d2024');
    setStyleProperty('--table-header-bg', '#1d2024');
    setStyleProperty('--table-stripe-bg', '#22262b');
    setStyleProperty('--table-border', '#2e3238');
    setStyleProperty('--tag-default-bg', '#2e3238');
    setStyleProperty('--input-bg', '#22262b');
    setStyleProperty('--header-shadow', '0 1px 3px rgba(0, 0, 0, 0.35)');
  } else {
    root.classList.remove('dark');
    // Light themes: use theme-tinted light sidebar (50% white mix) with dark text for WCAG AA contrast
    const sidebarBg = mix.light[5];
    const sidebarHover = mix.light[4];
    const sidebarText = '#1e293b';
    const pageBg = mix.light[7];
    const footerBg = mix.light[6]; // Slightly darker than page for visual hierarchy
    const cardHeaderBg = mix.light[8];
    const borderColor = mix.light[6];
    const tagDefaultBg = mix.light[7];
    const inputBg = '#ffffff';
    const headerShadow = '0 1px 3px rgba(0, 0, 0, 0.04)';

    setStyleProperty('--menubar-bg', sidebarBg);
    setStyleProperty('--menubar-hover-bg', sidebarHover);
    setStyleProperty('--menubar-text', sidebarText);
    setStyleProperty('--menubar-logo-text', theme.primary);
    setStyleProperty('--menubar-active-text', mix.dark[2]);
    setStyleProperty('--navbar-bg', '#ffffff');
    setStyleProperty('--navbar-border', borderColor);
    setStyleProperty('--page-bg', pageBg);
    setStyleProperty('--footer-bg', footerBg);
    setStyleProperty('--card-bg', '#ffffff');
    setStyleProperty('--card-header-bg', cardHeaderBg);
    setStyleProperty('--table-header-bg', cardHeaderBg);
    setStyleProperty('--table-stripe-bg', cardHeaderBg);
    setStyleProperty('--table-border', borderColor);
    setStyleProperty('--tag-default-bg', tagDefaultBg);
    setStyleProperty('--input-bg', inputBg);
    setStyleProperty('--header-shadow', headerShadow);
  }
}

function findTheme(themeId) {
  return THEMES.find((t) => t.id === themeId) || THEMES[0];
}

export const useSettingsStore = defineStore('settings', () => {
  const settingsVisible = ref(false);
  const showTabs = useStorage('inkwash-showTabs', defaultSettings.showTabs);
  const showLogo = useStorage('inkwash-showLogo', defaultSettings.showLogo);
  const showFooter = useStorage('inkwash-showFooter', defaultSettings.showFooter);
  const fixHeader = useStorage('inkwash-fixHeader', defaultSettings.fixHeader);
  const layout = useStorage('inkwash-layout', defaultSettings.layout);
  const theme = useStorage('inkwash-theme', 'sky-blue');
  const locale = useStorage('inkwash-locale', 'zh-CN');

  function applyThemeVars() {
    const current = findTheme(theme.value);
    setSurfaceVars(theme.value);
    const mix = genMixColor(current.primary);
    setStyleProperty('--el-color-primary', mix.DEFAULT);
    setStyleProperty('--el-color-primary-dark-2', mix.dark[2]);
    setStyleProperty('--el-color-primary-light-1', mix.light[1]);
    setStyleProperty('--el-color-primary-light-2', mix.light[2]);
    setStyleProperty('--el-color-primary-light-3', mix.light[3]);
    setStyleProperty('--el-color-primary-light-4', mix.light[4]);
    setStyleProperty('--el-color-primary-light-5', mix.light[5]);
    setStyleProperty('--el-color-primary-light-6', mix.light[6]);
    setStyleProperty('--el-color-primary-light-7', mix.light[7]);
    setStyleProperty('--el-color-primary-light-8', mix.light[8]);
    setStyleProperty('--el-color-primary-light-9', mix.light[9]);
  }

  watch(
    theme,
    () => {
      applyThemeVars();
    },
    { immediate: true },
  );

  function changeTheme(val) {
    theme.value = val;
  }
  function changeLayout(val) {
    layout.value = val;
  }
  function changeLocale(val) {
    locale.value = val;
    i18n.global.locale.value = val;
  }

  async function loadPreference() {
    try {
      const pref = await getPreference();
      if (!pref) return;
      theme.value = pref.theme || 'sky-blue';
      locale.value = pref.language || 'zh-CN';
      layout.value = pref.menuStyle || 'left';
      showTabs.value = pref.showTabs ?? true;
      showLogo.value = pref.showLogo ?? true;
      showFooter.value = pref.showFooter ?? true;
      fixHeader.value = pref.fixHeader ?? true;
    } catch {
      // 拉取失败时保持本地配置
    }
  }

  function syncPreference() {
    return savePreference({
      theme: theme.value,
      language: locale.value,
      menuStyle: layout.value,
      showTabs: showTabs.value,
      showLogo: showLogo.value,
      showFooter: showFooter.value,
      fixHeader: fixHeader.value,
    });
  }

  return {
    settingsVisible,
    showTabs,
    fixHeader,
    showLogo,
    showFooter,
    layout,
    theme,
    changeTheme,
    changeLayout,
    locale,
    changeLocale,
    loadPreference,
    syncPreference,
  };
});
