import { normalizePage, getMyFavorites, getMyAgreements, getMyDislikes, getMyCommented } from '@inkwash/share';
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
import { ref } from 'vue';

const PAGE_SIZE = 20;

const SOURCES = {
  favorites: getMyFavorites,
  agreements: getMyAgreements,
  averses: getMyDislikes,
  commented: getMyCommented,
};

export const useInteractionStore = defineStore('interaction', () => {
  const lists = {
    favorites: ref([]),
    agreements: ref([]),
    averses: ref([]),
    commented: ref([]),
  };
  const totals = {
    favorites: ref(0),
    agreements: ref(0),
    averses: ref(0),
    commented: ref(0),
  };
  const pages = {
    favorites: ref(1),
    agreements: ref(1),
    averses: ref(1),
    commented: ref(1),
  };
  const noMore = {
    favorites: ref(false),
    agreements: ref(false),
    averses: ref(false),
    commented: ref(false),
  };
  const loading = {
    favorites: ref(false),
    agreements: ref(false),
    averses: ref(false),
    commented: ref(false),
  };
  const generations = { favorites: 0, agreements: 0, averses: 0, commented: 0 };

  function createFetcher(key) {
    return async function fetchList({ reload = false, ...params } = {}) {
      const generation = ++generations[key];
      if (!reload && noMore[key].value) return;
      const nextPage = reload ? 1 : pages[key].value + 1;
      loading[key].value = true;
      try {
        const res = await SOURCES[key]({
          page: nextPage,
          size: PAGE_SIZE,
          ...params,
        });
        const { items, total } = normalizePage(res);
        if (generations[key] !== generation) return;
        lists[key].value = reload ? items : [...lists[key].value, ...items];
        totals[key].value = total;
        pages[key].value = nextPage;
        noMore[key].value = items.length === 0 || lists[key].value.length >= total;
      } catch (e) {
        if (generations[key] === generation) {
          lists[key].value = [];
          totals[key].value = 0;
          pages[key].value = 1;
          noMore[key].value = false;
        }
        throw e;
      } finally {
        if (generations[key] === generation) loading[key].value = false;
      }
    };
  }

  return {
    favorites: lists.favorites,
    agreements: lists.agreements,
    averses: lists.averses,
    commented: lists.commented,
    favoritesTotal: totals.favorites,
    agreementsTotal: totals.agreements,
    aversesTotal: totals.averses,
    commentedTotal: totals.commented,
    pages,
    noMore,
    loading,
    fetchFavorites: createFetcher('favorites'),
    fetchAgreements: createFetcher('agreements'),
    fetchAverses: createFetcher('averses'),
    fetchCommented: createFetcher('commented'),
  };
});
