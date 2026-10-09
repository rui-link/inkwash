<!--
This file is part of Inkwash.
Copyright (C) 2026 ruilink team.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->
<template>
  <div class="article-list container">
    <header class="list-header">
      <h1 class="list-title">{{ $t('articleList.title') }}</h1>
      <div class="chinese-divider" aria-hidden="true" />
      <p class="list-desc" v-if="store.searchKeyword">
        {{ $t('articleList.descSearch') }}
        <strong>"{{ store.searchKeyword }}"</strong>
      </p>
      <p class="list-desc" v-else>
        {{ $t('articleList.desc') }}
      </p>
    </header>

    <div v-if="store.loading" class="loading-state">
      <div class="skeleton-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
              <div style="padding: 20px">
                <el-skeleton-item variant="text" style="width: 40%; margin-bottom: 12px" />
                <el-skeleton-item variant="text" style="width: 100%; margin-bottom: 8px" />
                <el-skeleton-item variant="text" style="width: 80%; margin-bottom: 16px" />
                <el-skeleton-item variant="text" style="width: 60%" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>
    </div>

    <div v-else-if="store.articles.length === 0" class="empty-state">
      <el-empty :description="store.searchKeyword ? $t('articleList.noResults') : $t('articleList.noArticles')" />
    </div>

    <div v-else class="article-grid">
      <ArticleCard v-for="article in store.articles" :key="article.id" :article="article" />
    </div>

    <div class="pagination-wrapper" v-if="store.total > store.pageSize">
      <el-pagination
        v-model:current-page="store.currentPage"
        :page-size="store.pageSize"
        :total="store.total"
        layout="prev, pager, next"
        @current-change="handlePageChange" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

import ArticleCard from '@/components/ArticleCard.vue';
import { useArticleStore } from '@/stores/article';

const route = useRoute();
const store = useArticleStore();

onMounted(() => {
  store.searchKeyword = route.query.search || '';
  store.fetchArticles({ search: store.searchKeyword });
});

watch(
  () => route.query.search,
  (val) => {
    store.searchKeyword = val || '';
    store.currentPage = 1;
    store.fetchArticles({ search: store.searchKeyword });
  },
);

function handlePageChange(page) {
  store.currentPage = page;
  store.fetchArticles({ search: store.searchKeyword });
}
</script>

<style scoped>
.article-list {
  padding-top: 8px;
}

.list-header {
  margin-bottom: clamp(20px, 4vw, 32px);
  text-align: center;
}

.list-title {
  font-family: var(--font-display);
  font-size: clamp(24px, 3.5vw, 32px);
  font-weight: 800;
  letter-spacing: -0.03em;
  margin-bottom: 8px;
}

.list-desc {
  font-size: 15px;
  color: var(--text-secondary);
  max-width: 500px;
  margin: 0 auto;
}

.list-header .chinese-divider {
  max-width: 80px;
  margin: 12px auto;
}

.loading-state {
  padding: 20px 0;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.skeleton-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.empty-state {
  padding: 80px 0;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .article-grid {
    grid-template-columns: 1fr;
  }
  .skeleton-grid {
    grid-template-columns: 1fr;
  }
}
</style>
