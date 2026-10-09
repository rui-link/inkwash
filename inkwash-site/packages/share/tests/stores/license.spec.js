import { createPinia, setActivePinia } from 'pinia';
import { describe, it, expect, vi, beforeEach } from 'vitest';

import { getLicenseInfo } from '../../src/api/system.js';
import { useLicenseStore } from '../../src/stores/license.js';

vi.mock('../../src/api/system.js', () => ({
  getLicenseInfo: vi.fn(),
}));

describe('useLicenseStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('starts with default state', () => {
    const store = useLicenseStore();

    expect(store.edition).toBe('');
    expect(store.maxUsers).toBeNull();
    expect(store.licensed).toBe(true);
  });

  it('stores edition, maxUsers and licensed from the API response', async () => {
    getLicenseInfo.mockResolvedValue({
      licensed: true,
      edition: 'trial',
      maxUsers: 5,
    });
    const store = useLicenseStore();

    await store.fetchLicenseInfo();

    expect(getLicenseInfo).toHaveBeenCalledTimes(1);
    expect(store.edition).toBe('trial');
    expect(store.maxUsers).toBe(5);
    expect(store.licensed).toBe(true);
  });

  it('treats a missing licensed flag as licensed and missing edition as empty', async () => {
    getLicenseInfo.mockResolvedValue({ maxUsers: null });
    const store = useLicenseStore();

    await store.fetchLicenseInfo();

    expect(store.licensed).toBe(true);
    expect(store.edition).toBe('');
    expect(store.maxUsers).toBeNull();
  });

  it('marks the install unlicensed when licensed is explicitly false', async () => {
    getLicenseInfo.mockResolvedValue({
      licensed: false,
      edition: '',
      maxUsers: 0,
    });
    const store = useLicenseStore();

    await store.fetchLicenseInfo();

    expect(store.licensed).toBe(false);
    expect(store.maxUsers).toBe(0);
  });

  it('keeps current state when the request fails', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    getLicenseInfo.mockRejectedValue(new Error('network down'));
    const store = useLicenseStore();
    store.edition = 'trial';
    store.maxUsers = 5;

    await store.fetchLicenseInfo();

    expect(store.edition).toBe('trial');
    expect(store.maxUsers).toBe(5);
    expect(errorSpy).toHaveBeenCalledWith('Failed to fetch license info:', expect.any(Error));
    errorSpy.mockRestore();
  });

  it('ignores a falsy response without changing state', async () => {
    getLicenseInfo.mockResolvedValue(null);
    const store = useLicenseStore();
    store.edition = 'pro';
    store.maxUsers = 10;

    await store.fetchLicenseInfo();

    expect(store.edition).toBe('pro');
    expect(store.maxUsers).toBe(10);
    expect(store.licensed).toBe(true);
  });
});
