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
  <article
    class="article-card ornament-card"
    role="link"
    tabindex="0"
    @click="$router.push(`/article/${article.id}`)"
    @keydown.enter="$router.push(`/article/${article.id}`)">
    <div class="card-image" v-if="article.coverUrl">
      <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
    </div>
    <div class="card-image card-image-placeholder" v-else>
      <AppIcon name="brandFaint" :size="48" />
    </div>
    <div class="card-body">
      <div class="card-meta">
        <span class="meta-author">{{ article.author }}</span>
        <span class="meta-sep">·</span>
        <span class="meta-date">{{ formatDate(article.publishTime || article.createTime) }}</span>
      </div>
      <h3 class="card-title">{{ article.title }}</h3>
      <p class="card-summary" v-if="article.summary">
        {{ truncate(article.summary, 120) }}
      </p>
      <div class="card-stats" v-if="article.tally">
        <span class="stat">
          <AppIcon name="eye" :size="14" />
          {{ article.tally.viewCount || 0 }}
        </span>
        <span class="stat">
          <AppIcon name="comment" :size="14" />
          {{ article.tally.commentCount || 0 }}
        </span>
        <span class="stat">
          <AppIcon name="thumbUp" :size="14" />
          {{ article.tally.agreeCount || 0 }}
        </span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { formatDate } from '@inkwash/share';

import AppIcon from '@/components/AppIcon.vue';
import { truncate } from '@/utils/helpers';

defineProps({
  article: { type: Object, required: true },
});
</script>

<style scoped>
.article-card {
  display: flex;
  flex-direction: column;
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.article-card:hover {
  border-color: var(--accent-color);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.card-image {
  position: relative;
  width: 100%;
  height: clamp(160px, 20vw, 200px);
  overflow: hidden;
  background: var(--bg-secondary);
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.article-card:hover .card-image img {
  transform: scale(1.05);
}

.card-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.card-image-placeholder svg {
  width: 48px;
  height: 48px;
}

.card-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.meta-sep {
  opacity: 0.4;
}

.card-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 8px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
  flex: 1;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: auto;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat svg {
  opacity: 0.5;
}
</style>
