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

export const useTabviewStore = defineStore('tabview', () => {
  const visitedViews = ref([]);
  const cachedViews = ref([]);

  function addVisitedView(view) {
    if (visitedViews.value.some((v) => v.path === view.path)) return;
    if (view.affix) {
      visitedViews.value.unshift(view);
    } else {
      visitedViews.value.push(view);
    }
  }

  function addCachedView(view) {
    if (cachedViews.value.includes(view.name)) return;
    if (view.keepAlive) cachedViews.value.push(view.name);
  }

  function delVisitedView(view) {
    for (const [i, v] of visitedViews.value.entries()) {
      if (v.path === view.path) {
        visitedViews.value.splice(i, 1);
        break;
      }
    }
    return [...visitedViews.value];
  }

  function delCachedView(view) {
    const index = cachedViews.value.indexOf(view.name);
    if (index > -1) cachedViews.value.splice(index, 1);
    return [...cachedViews.value];
  }

  function delOtherVisitedViews(view) {
    visitedViews.value = visitedViews.value.filter((v) => v?.affix || v.path === view.path);
    return [...visitedViews.value];
  }

  function delOtherCachedViews(view) {
    const viewName = view.name;
    const index = cachedViews.value.indexOf(viewName);
    if (index > -1) {
      cachedViews.value = cachedViews.value.slice(index, index + 1);
    } else {
      cachedViews.value = [];
    }
    return [...cachedViews.value];
  }

  function updateVisitedView(view) {
    for (const v of visitedViews.value) {
      if (v.path === view.path) {
        Object.assign(v, view);
        break;
      }
    }
  }

  function addView(view) {
    addVisitedView(view);
    addCachedView(view);
  }

  function delView(view) {
    delVisitedView(view);
    delCachedView(view);
    return {
      visitedViews: [...visitedViews.value],
      cachedViews: [...cachedViews.value],
    };
  }

  function delOtherViews(view) {
    delOtherVisitedViews(view);
    delOtherCachedViews(view);
    return {
      visitedViews: [...visitedViews.value],
      cachedViews: [...cachedViews.value],
    };
  }

  function delLeftViews(view) {
    const currIndex = visitedViews.value.findIndex((v) => v.path === view.path);
    if (currIndex === -1) return { visitedViews: [...visitedViews.value] };
    visitedViews.value = visitedViews.value.filter((item, index) => {
      if (index >= currIndex || item?.affix) return true;
      const cacheIndex = cachedViews.value.indexOf(item.name);
      if (cacheIndex > -1) cachedViews.value.splice(cacheIndex, 1);
      return false;
    });
    return { visitedViews: [...visitedViews.value] };
  }

  function delRightViews(view) {
    const currIndex = visitedViews.value.findIndex((v) => v.path === view.path);
    if (currIndex === -1) return { visitedViews: [...visitedViews.value] };
    const removed = visitedViews.value.filter((item, index) => index > currIndex && !item?.affix);
    visitedViews.value = visitedViews.value.filter((item, index) => index <= currIndex || item?.affix);
    for (const item of removed) {
      const cacheIndex = cachedViews.value.indexOf(item.name);
      if (cacheIndex > -1) cachedViews.value.splice(cacheIndex, 1);
    }
    return {
      visitedViews: [...visitedViews.value],
      cachedViews: [...cachedViews.value],
    };
  }

  function delAllViews() {
    const affixTags = visitedViews.value.filter((tag) => tag?.affix);
    visitedViews.value = affixTags;
    cachedViews.value = [];
    return {
      visitedViews: [...visitedViews.value],
      cachedViews: [...cachedViews.value],
    };
  }

  return {
    visitedViews,
    cachedViews,
    addVisitedView,
    addCachedView,
    delVisitedView,
    delCachedView,
    delOtherVisitedViews,
    delOtherCachedViews,
    updateVisitedView,
    addView,
    delView,
    delOtherViews,
    delLeftViews,
    delRightViews,
    delAllViews,
  };
});
