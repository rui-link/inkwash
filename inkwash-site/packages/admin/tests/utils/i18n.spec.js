import en from '@inkwash/share/locale/lang/en.js';
import zhCN from '@inkwash/share/locale/lang/zh-CN.js';
import { describe, expect, it } from 'vitest';

import i18n from '@/locale/index';
import { translateRouteTitle } from '@/utils/i18n';
import { MENU_TITLE_KEYS } from '@/utils/menuKeys';

describe('i18n menu keys (unified to English)', () => {
  it('zh-CN and en menu sections have identical key sets and no Chinese keys', () => {
    const zhKeys = Object.keys(zhCN.menu).sort();
    const enKeys = Object.keys(en.menu).sort();
    expect(zhKeys).toEqual(enKeys);
    expect(zhKeys.length).toBeGreaterThan(0);
    expect(zhKeys.join('')).not.toMatch(/[\u4e00-\u9fff]/);
  });

  it('bridges every Chinese backend title to an existing English menu key', () => {
    const dictKeys = new Set(Object.keys(en.menu));
    for (const [zhTitle, enKey] of Object.entries(MENU_TITLE_KEYS)) {
      expect(dictKeys.has(enKey)).toBe(true);
      expect(MENU_TITLE_KEYS[zhTitle]).toBe(enKey);
    }
    expect(new Set(Object.values(MENU_TITLE_KEYS))).toEqual(dictKeys);
  });

  it('translateRouteTitle resolves a Chinese backend title through the bridge', () => {
    i18n.global.locale.value = 'en';
    expect(translateRouteTitle('内容管理')).toBe('Content');
    expect(translateRouteTitle('首页')).toBe('Home');
    expect(translateRouteTitle('权限管理')).toBe('Permissions');
    i18n.global.locale.value = 'zh-CN';
    expect(translateRouteTitle('内容管理')).toBe('内容管理');
    expect(translateRouteTitle('首页')).toBe('首页');
  });

  it('translateRouteTitle still translates English menu keys and passes through unknown titles', () => {
    i18n.global.locale.value = 'en';
    expect(translateRouteTitle('home')).toBe('Home');
    expect(translateRouteTitle('not-a-menu-title')).toBe('not-a-menu-title');
    i18n.global.locale.value = 'zh-CN';
  });
});
