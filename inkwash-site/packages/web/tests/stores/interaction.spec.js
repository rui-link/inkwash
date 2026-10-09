import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useInteractionStore } from '@/stores/interaction';

const { getMyFavorites, getMyAgreements, getMyDislikes, getMyCommented } = vi.hoisted(() => ({
  getMyFavorites: vi.fn(),
  getMyAgreements: vi.fn(),
  getMyDislikes: vi.fn(),
  getMyCommented: vi.fn(),
}));

vi.mock('@inkwash/share', () => ({
  getMyFavorites,
  getMyAgreements,
  getMyDislikes,
  getMyCommented,
  normalizePage: (res) => {
    if (!res || typeof res !== 'object') return { items: [], total: 0 };
    const items = Array.isArray(res.list) ? res.list : [];
    const rawTotal = res.total ?? items.length;
    const total = Number.isFinite(Number(rawTotal)) && rawTotal !== '' ? Number(rawTotal) : items.length;
    return { items, total };
  },
}));

function paged(items, total) {
  return { list: items, total };
}

function listOf(size, offset = 0) {
  return Array.from({ length: size }, (_, i) => ({ id: offset + i }));
}

describe('web interaction store pagination', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
  });

  it('reload fetches page 1 with size 20 and replaces the list', async () => {
    getMyFavorites.mockResolvedValue(paged([{ id: 1 }], 45));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });

    expect(getMyFavorites).toHaveBeenCalledWith({ page: 1, size: 20 });
    expect(store.favorites).toEqual([{ id: 1 }]);
    expect(store.favoritesTotal).toBe(45);
    expect(store.pages.favorites).toBe(1);
    expect(store.noMore.favorites).toBe(false);
    expect(store.loading.favorites).toBe(false);
  });

  it('load-more requests the next page and appends without replacing', async () => {
    getMyFavorites.mockResolvedValueOnce(paged(listOf(20, 1), 45)).mockResolvedValueOnce(paged(listOf(20, 21), 45));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });
    await store.fetchFavorites();

    expect(getMyFavorites).toHaveBeenLastCalledWith({ page: 2, size: 20 });
    expect(store.favorites).toHaveLength(40);
    expect(store.favorites[39]).toEqual({ id: 40 });
    expect(store.pages.favorites).toBe(2);
    expect(store.noMore.favorites).toBe(false);
  });

  it('marks noMore when the final partial page completes the total', async () => {
    getMyFavorites
      .mockResolvedValueOnce(paged(listOf(20, 1), 45))
      .mockResolvedValueOnce(paged(listOf(20, 21), 45))
      .mockResolvedValueOnce(paged(listOf(5, 41), 45));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });
    await store.fetchFavorites();
    await store.fetchFavorites();

    expect(getMyFavorites).toHaveBeenLastCalledWith({ page: 3, size: 20 });
    expect(store.favorites).toHaveLength(45);
    expect(store.pages.favorites).toBe(3);
    expect(store.noMore.favorites).toBe(true);
  });

  it('marks noMore when a load-more page returns no items', async () => {
    getMyFavorites.mockResolvedValueOnce(paged(listOf(20, 1), 45)).mockResolvedValueOnce(paged([], 45));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });
    await store.fetchFavorites();

    expect(store.noMore.favorites).toBe(true);
  });

  it('marks noMore immediately when the whole list fits the first page', async () => {
    getMyFavorites.mockResolvedValue(paged([{ id: 1 }], 1));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });

    expect(store.noMore.favorites).toBe(true);
  });

  it('ignores load-more once the list is exhausted', async () => {
    getMyFavorites.mockResolvedValue(paged([{ id: 1 }], 1));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });
    await store.fetchFavorites();
    await store.fetchFavorites();

    expect(getMyFavorites).toHaveBeenCalledTimes(1);
  });

  it('reload from a later page resets the page and replaces the list', async () => {
    getMyFavorites
      .mockResolvedValueOnce(paged(listOf(20, 1), 45))
      .mockResolvedValueOnce(paged(listOf(20, 21), 45))
      .mockResolvedValueOnce(paged(listOf(20, 1), 45));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });
    await store.fetchFavorites();
    await store.fetchFavorites({ reload: true });

    expect(getMyFavorites).toHaveBeenLastCalledWith({ page: 1, size: 20 });
    expect(store.favorites).toHaveLength(20);
    expect(store.pages.favorites).toBe(1);
    expect(store.noMore.favorites).toBe(false);
  });

  it('clears the list and resets the page when a fetch fails', async () => {
    getMyFavorites.mockRejectedValue(new Error('boom'));
    const store = useInteractionStore();

    await expect(store.fetchFavorites({ reload: true })).rejects.toThrow('boom');

    expect(store.favorites).toEqual([]);
    expect(store.favoritesTotal).toBe(0);
    expect(store.pages.favorites).toBe(1);
    expect(store.noMore.favorites).toBe(false);
    expect(store.loading.favorites).toBe(false);
  });

  it('merges extra params while keeping page and size authoritative', async () => {
    getMyCommented.mockResolvedValue(paged([], 0));
    const store = useInteractionStore();

    await store.fetchCommented({ reload: true, keyword: '墨水' });

    expect(getMyCommented).toHaveBeenCalledWith({
      page: 1,
      size: 20,
      keyword: '墨水',
    });
  });

  it('ignores an outdated load-more response after a newer reload', async () => {
    let resolvePage2;
    getMyFavorites
      .mockResolvedValueOnce(paged(listOf(20, 1), 45))
      .mockImplementationOnce(() => new Promise((resolve) => (resolvePage2 = resolve)))
      .mockResolvedValueOnce(paged(listOf(20, 1), 45));
    const store = useInteractionStore();

    await store.fetchFavorites({ reload: true });
    const page2 = store.fetchFavorites();
    await store.fetchFavorites({ reload: true });

    resolvePage2(paged(listOf(20, 21), 45));
    await page2;

    expect(store.favorites).toHaveLength(20);
    expect(store.pages.favorites).toBe(1);
  });
});
