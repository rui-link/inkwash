import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@vueuse/core', async () => {
  const { ref } = await import('vue');
  return {
    useStorage: (key, initial) => ref(initial),
  };
});

const { getPreference, savePreference } = vi.hoisted(() => ({
  getPreference: vi.fn(),
  savePreference: vi.fn(),
}));

vi.mock('@inkwash/share', () => ({
  THEMES: [
    { id: 'sky-blue', name: '天空蓝', primary: '#0070f3', secondary: '#07a' },
    { id: 'ink-dark', name: '墨色', primary: '#15171a', secondary: '#1d2024' },
  ],
  getPreference,
  savePreference,
}));

import { useSettingsStore } from '@/stores/settings';

describe('useSettingsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('loadPreference 用后端返回覆盖 store 各值', async () => {
    getPreference.mockResolvedValue({
      theme: 'ink-dark',
      language: 'en',
      menuStyle: 'top',
      showTabs: false,
      showLogo: true,
      showFooter: false,
      fixHeader: true,
    });
    const store = useSettingsStore();
    store.theme = 'sky-blue';

    await store.loadPreference();

    expect(getPreference).toHaveBeenCalledTimes(1);
    expect(store.theme).toBe('ink-dark');
    expect(store.locale).toBe('en');
    expect(store.layout).toBe('top');
    expect(store.showTabs).toBe(false);
    expect(store.showFooter).toBe(false);
  });

  it('loadPreference 无记录时保持现状（不抛错）', async () => {
    getPreference.mockResolvedValue(null);
    const store = useSettingsStore();

    await expect(store.loadPreference()).resolves.toBeUndefined();
  });

  it('syncPreference 用当前 store 值构建 payload 并调用 savePreference', async () => {
    savePreference.mockResolvedValue({});
    const store = useSettingsStore();
    store.theme = 'ink-dark';
    store.locale = 'en';
    store.layout = 'top';
    store.showTabs = false;

    await store.syncPreference();

    expect(savePreference).toHaveBeenCalledWith({
      theme: 'ink-dark',
      language: 'en',
      menuStyle: 'top',
      showTabs: false,
      showLogo: true,
      showFooter: true,
      fixHeader: true,
    });
  });

  it('syncPreference 失败时 reject（不吞错）', async () => {
    savePreference.mockRejectedValue(new Error('network'));
    const store = useSettingsStore();

    await expect(store.syncPreference()).rejects.toThrow('network');
  });

  it('修改设置不会触发自动保存（移除了 watch 自动保存）', async () => {
    const store = useSettingsStore();
    store.showTabs = false;
    store.theme = 'ink-dark';
    await Promise.resolve();

    expect(savePreference).not.toHaveBeenCalled();
  });
});
