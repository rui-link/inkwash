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
  <div class="my-interactions">
    <div class="page-header">
      <h2 class="page-title">{{ $t('myInteractions.title') }}</h2>
      <p class="page-desc">{{ $t('myInteractions.desc') }}</p>
    </div>

    <div class="interactions-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="$t('myInteractions.commented')" name="commented">
          <div v-loading="store.loading.commented">
            <div v-if="store.commented.length === 0" class="empty-state">
              <el-empty :description="$t('myInteractions.noData')" />
            </div>
            <div v-else class="article-grid">
              <ArticleCard v-for="a in store.commented" :key="a.id" :article="a" />
            </div>
          </div>
          <div v-if="!store.noMore.commented && store.commented.length > 0" class="load-more-wrap">
            <el-button
              class="load-more"
              size="small"
              :loading="store.loading.commented"
              @click="store.fetchCommented()">
              {{ $t('myInteractions.loadMore') }}
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="$t('myInteractions.agreed')" name="agreements">
          <div v-loading="store.loading.agreements">
            <div v-if="store.agreements.length === 0" class="empty-state">
              <el-empty :description="$t('myInteractions.noData')" />
            </div>
            <div v-else class="article-grid">
              <ArticleCard v-for="a in store.agreements" :key="a.id" :article="a" />
            </div>
          </div>
          <div v-if="!store.noMore.agreements && store.agreements.length > 0" class="load-more-wrap">
            <el-button
              class="load-more"
              size="small"
              :loading="store.loading.agreements"
              @click="store.fetchAgreements()">
              {{ $t('myInteractions.loadMore') }}
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="$t('myInteractions.aversed')" name="averses">
          <div v-loading="store.loading.averses">
            <div v-if="store.averses.length === 0" class="empty-state">
              <el-empty :description="$t('myInteractions.noData')" />
            </div>
            <div v-else class="article-grid">
              <ArticleCard v-for="a in store.averses" :key="a.id" :article="a" />
            </div>
          </div>
          <div v-if="!store.noMore.averses && store.averses.length > 0" class="load-more-wrap">
            <el-button class="load-more" size="small" :loading="store.loading.averses" @click="store.fetchAverses()">
              {{ $t('myInteractions.loadMore') }}
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="$t('myInteractions.favorited')" name="favorites">
          <div v-loading="store.loading.favorites">
            <div v-if="store.favorites.length === 0" class="empty-state">
              <el-empty :description="$t('myInteractions.noData')" />
            </div>
            <div v-else class="article-grid">
              <ArticleCard v-for="a in store.favorites" :key="a.id" :article="a" />
            </div>
          </div>
          <div v-if="!store.noMore.favorites && store.favorites.length > 0" class="load-more-wrap">
            <el-button
              class="load-more"
              size="small"
              :loading="store.loading.favorites"
              @click="store.fetchFavorites()">
              {{ $t('myInteractions.loadMore') }}
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { getErrorMessage } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import ArticleCard from '@/components/ArticleCard.vue';
import { useInteractionStore } from '@/stores/interaction';

const store = useInteractionStore();
const { t } = useI18n();
const activeTab = ref('commented');

onMounted(() => handleTabChange('commented'));

async function handleTabChange(tab) {
  try {
    switch (tab) {
      case 'commented':
        await store.fetchCommented({ reload: true });
        break;
      case 'agreements':
        await store.fetchAgreements({ reload: true });
        break;
      case 'averses':
        await store.fetchAverses({ reload: true });
        break;
      case 'favorites':
        await store.fetchFavorites({ reload: true });
        break;
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('myInteractions.failed')));
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--text-secondary);
}

.interactions-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 24px;
}

.interactions-card :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.interactions-card :deep(.el-tabs__item) {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  height: 40px;
}

.interactions-card :deep(.el-tabs__item.is-active) {
  color: var(--accent-color);
}

.interactions-card :deep(.el-tabs__active-bar) {
  background: var(--accent-color);
}

.empty-state {
  padding: 40px 0;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.load-more-wrap {
  display: flex;
  justify-content: center;
  padding-top: 20px;
}
</style>
