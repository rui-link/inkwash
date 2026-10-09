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
import { computed } from 'vue';

// Theme definitions with metadata
export const THEMES = [
  {
    id: 'chinese-red',
    name: '中国红',
    nameEn: 'Chinese Red',
    primary: '#8b2516',
    secondary: '#9d3e2b',
    description: '朱砂红，热烈庄重',
    icon: '🏮',
  },
  {
    id: 'sky-blue',
    name: '海天蓝',
    nameEn: 'Sky Blue',
    primary: '#266d8a',
    secondary: '#3d7d95',
    description: '天青蓝，宁静悠远',
    icon: '🌊',
  },
  {
    id: 'natural-green',
    name: '原野绿',
    nameEn: 'Natural Green',
    primary: '#4a7c59',
    secondary: '#679872',
    description: '竹青绿，自然清新',
    icon: '🌿',
  },
  {
    id: 'harvest-yellow',
    name: '丰收黄',
    nameEn: 'Harvest Yellow',
    primary: '#9a651a',
    secondary: '#b5821f',
    description: '琥珀金，温润丰收',
    icon: '🌾',
  },
  {
    id: 'ink-dark',
    name: '玄墨',
    nameEn: 'Ink Dark',
    primary: '#c89b62',
    secondary: '#dcb87e',
    description: '玄墨鎏金，水墨意境',
    icon: '🖌️',
  },
];

export const useAppStore = defineStore('app', () => {
  const device = useStorage('inkwash-device', 'desktop');
  const size = useStorage('inkwash-size', 'default');
  const locale = useStorage('inkwash-locale', 'zh-CN');
  const menuStatus = useStorage('inkwash-menu-status', 'opened');
  const theme = useStorage('inkwash-theme', 'sky-blue');
  const isMobile = computed(() => device.value === 'mobile');

  // Get current theme metadata
  const currentTheme = computed(() => {
    return THEMES.find((t) => t.id === theme.value) || THEMES[0];
  });

  function toggleDevice(val) {
    device.value = val || (device.value === 'desktop' ? 'mobile' : 'desktop');
  }

  function changeSize(val) {
    size.value = val;
  }
  function changeLocale(val) {
    locale.value = val;
  }

  function toggleMenuBar() {
    menuStatus.value = menuStatus.value === 'opened' ? 'closed' : 'opened';
  }
  function openMenuBar() {
    menuStatus.value = 'opened';
  }
  function closeMenuBar() {
    menuStatus.value = 'closed';
  }

  const activeTopMenuPath = useStorage('inkwash-activeTopMenuPath', '');
  function activeTopMenu(val) {
    activeTopMenuPath.value = val;
  }

  // Theme management
  function setTheme(newTheme) {
    theme.value = newTheme;
    applyTheme();
  }

  function applyTheme() {
    const root = document.documentElement;
    if (root) {
      root.setAttribute('data-theme', theme.value);
    }
  }

  function initTheme() {
    applyTheme();
  }

  return {
    device,
    size,
    locale,
    menuStatus,
    theme,
    isMobile,
    currentTheme,
    toggleDevice,
    changeSize,
    changeLocale,
    toggleMenuBar,
    openMenuBar,
    closeMenuBar,
    activeTopMenuPath,
    activeTopMenu,
    setTheme,
    applyTheme,
    initTheme,
  };
});
