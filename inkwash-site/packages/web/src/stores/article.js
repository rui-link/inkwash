import {
  getPublicArticles,
  getPublicArticle,
  getMyArticles,
  createUserArticle,
  updateUserArticle,
} from '@inkwash/share';
import { normalizePage } from '@inkwash/share';
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

export const useArticleStore = defineStore('article', () => {
  const articles = ref([]);
  const total = ref(0);
  const currentPage = ref(1);
  const pageSize = ref(20);
  const searchKeyword = ref('');
  const currentArticle = ref(null);
  const loading = ref(false);

  async function fetchArticles(params = {}) {
    loading.value = true;
    try {
      const res = await getPublicArticles({
        page: currentPage.value,
        size: pageSize.value,
        ...params,
      });
      const { items, total: totalCount } = normalizePage(res);
      articles.value = items;
      total.value = totalCount;
    } catch (e) {
      articles.value = [];
      total.value = 0;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchArticle(id) {
    loading.value = true;
    try {
      currentArticle.value = await getPublicArticle(id);
    } catch (e) {
      currentArticle.value = null;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchMyArticles(params = {}) {
    loading.value = true;
    try {
      const res = await getMyArticles({
        page: currentPage.value,
        size: pageSize.value,
        ...params,
      });
      const { items, total: totalCount } = normalizePage(res);
      articles.value = items;
      total.value = totalCount;
    } catch (e) {
      articles.value = [];
      total.value = 0;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function saveArticle(data, id = null) {
    if (id) {
      return await updateUserArticle(id, data);
    } else {
      return await createUserArticle(data);
    }
  }

  function clearArticle() {
    currentArticle.value = null;
  }

  return {
    articles,
    total,
    currentPage,
    pageSize,
    searchKeyword,
    currentArticle,
    loading,
    fetchArticles,
    fetchArticle,
    fetchMyArticles,
    saveArticle,
    clearArticle,
  };
});
