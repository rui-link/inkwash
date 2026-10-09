import { describe, it, expect } from 'vitest';

import { normalizePage, ensureArray } from '../../src/utils/page.js';

describe('ensureArray', () => {
  it('returns the value when it is already an array', () => {
    const arr = [1, 2];
    expect(ensureArray(arr)).toBe(arr);
  });

  it('returns empty array for non-array values', () => {
    expect(ensureArray(null)).toEqual([]);
    expect(ensureArray(undefined)).toEqual([]);
    expect(ensureArray({})).toEqual([]);
    expect(ensureArray('list')).toEqual([]);
    expect(ensureArray(5)).toEqual([]);
  });
});

describe('normalizePage', () => {
  it('returns empty result for null/undefined/primitive responses', () => {
    expect(normalizePage(null)).toEqual({ items: [], total: 0 });
    expect(normalizePage(undefined)).toEqual({ items: [], total: 0 });
    expect(normalizePage('oops')).toEqual({ items: [], total: 0 });
  });

  it('normalizes list-style responses', () => {
    const items = [{ id: 1 }];
    expect(normalizePage({ list: items, total: 10 })).toEqual({
      items,
      total: 10,
    });
  });

  // ISS-064: these two used to assert that `content` / `items` / `totalElements` /
  // `count` were accepted. No endpoint ever emitted them; the tolerance is gone and
  // the assertions now pin the contract to `list` + `total` only.
  it('no longer accepts content/items aliases — contract is list only', () => {
    expect(normalizePage({ content: [1], total: 1 }).items).toEqual([]);
    expect(normalizePage({ items: [2], total: 1 }).items).toEqual([]);
  });

  it('no longer accepts totalElements/count aliases — contract is total only', () => {
    expect(normalizePage({ list: [], totalElements: 7 }).total).toBe(0);
    expect(normalizePage({ list: [], count: 3 }).total).toBe(0);
  });

  it('keeps zero total without falling through', () => {
    expect(normalizePage({ list: [1], total: 0 }).total).toBe(0);
  });

  it('coerces numeric-string totals to numbers', () => {
    expect(normalizePage({ list: [], total: '42' }).total).toBe(42);
  });

  it('falls back to item count when total is missing or invalid', () => {
    expect(normalizePage({ list: [1, 2, 3] }).total).toBe(3);
    expect(normalizePage({ list: [1], total: 'abc' }).total).toBe(1);
  });

  it('coerces non-array page data to empty items instead of leaking bad shapes', () => {
    expect(normalizePage({ list: 'not-an-array', total: 5 })).toEqual({
      items: [],
      total: 5,
    });
  });
});
