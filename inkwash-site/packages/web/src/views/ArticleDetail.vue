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
  <div class="article-detail">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="!article" class="empty-state">
      <el-empty :description="$t('articleDetail.notFound')" />
    </div>

    <template v-else>
      <article class="article">
        <header class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span class="meta-author">{{ article.author }}</span>
            <span class="meta-sep">·</span>
            <span class="meta-date">{{ formatDate(article.publishTime || article.createTime) }}</span>
          </div>
          <div class="article-tags" v-if="article.terms?.length">
            <el-tag v-for="tag in article.terms" :key="tag.id" size="small" class="article-tag">{{ tag.name }}</el-tag>
          </div>
          <div class="chinese-divider" aria-hidden="true" />
          <p class="article-summary" v-if="article.summary">
            {{ article.summary }}
          </p>
        </header>

        <div class="article-cover" v-if="article.coverUrl">
          <img :src="article.coverUrl" :alt="article.title" />
        </div>

        <div class="article-content ornament-card">
          <div class="md-content" v-html="renderMarkdown(article.content || '')" />
        </div>

        <footer class="article-footer">
          <InteractionBar :article-id="article.id" :tally="article.tally" />
        </footer>
      </article>

      <section v-if="isPublished" class="comment-section">
        <div class="comment-header">
          <h2 class="comment-title">{{ $t('articleDetail.comments') }}</h2>
          <span class="comment-count">{{ article.tally?.commentCount || 0 }}</span>
        </div>

        <CommentForm
          ref="commentFormRef"
          v-if="authStore.isLoggedIn"
          :reply-to="replyTo"
          @submit="handleSubmitComment"
          @cancel-reply="handleCancelReply" />
        <div v-else class="login-hint">
          <el-button text @click="$router.push({ name: 'Login' })">
            {{ $t('articleDetail.signInToComment') }}
          </el-button>
        </div>

        <div v-if="comments.length === 0" class="empty-comments">
          {{ $t('articleDetail.noComments') }}
        </div>

        <div class="comments-list">
          <CommentItem v-for="comment in comments" :key="comment.id" :comment="comment" @reply="handleReply" />
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import {
  useAuthStore,
  getPublicArticle,
  getPublicComments,
  addComment,
  getErrorMessage,
  formatDate,
  ArticleStatus,
} from '@inkwash/share';
import { renderMarkdown } from '@inkwash/share';
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
// renderMarkdown uses DOMPurify.sanitize(md.render(content)) — safe for v-html.
// Never bypass or weaken this sanitization.

import CommentForm from '@/components/CommentForm.vue';
import CommentItem from '@/components/CommentItem.vue';
import InteractionBar from '@/components/InteractionBar.vue';

const route = useRoute();
const authStore = useAuthStore();
const { t } = useI18n();

const article = ref(null);
const comments = ref([]);
const loading = ref(true);
const replyTo = ref(null);
const commentFormRef = ref(null);

// Comments exist only on published articles: Article.canReply() is false otherwise, and
// CommentServiceImpl.getComments answers 403 for any non-published id on purpose, so that
// draft / pending / retracted comments cannot be enumerated. The http interceptor turns
// that 403 into a redirect to /403, which would otherwise bounce the author off their own
// preview. Gating the request and the comment section on the same condition keeps the page
// honest: an article we cannot fetch comments for shows no comment UI at all, rather than
// an empty list that implies "no comments" when they exist but are unreadable.
const isPublished = computed(() => article.value?.status === ArticleStatus.PUBLISHED);

onMounted(async () => {
  try {
    article.value = await getPublicArticle(route.params.id);
    if (isPublished.value) {
      comments.value = await getPublicComments(route.params.id);
    }
  } catch {
    article.value = null;
  } finally {
    loading.value = false;
  }
});

async function handleSubmitComment({ content, parentId, done }) {
  try {
    await addComment(article.value.id, {
      content,
      articleId: article.value.id,
      parentId,
    });
    comments.value = await getPublicComments(route.params.id);
    if (article.value.tally) {
      article.value.tally.commentCount = (article.value.tally.commentCount || 0) + 1;
    }
    replyTo.value = null;
    done?.();
  } catch (e) {
    done?.(getErrorMessage(e, t('commentForm.submitFailed')));
  }
}

function handleReply(comment) {
  replyTo.value = comment;
  commentFormRef.value?.$el?.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  });
}

function handleCancelReply() {
  replyTo.value = null;
}
</script>

<style scoped>
.article-detail {
  max-width: 740px;
  margin: 0 auto;
  padding: 0 clamp(16px, 3vw, 24px);
}

.loading-state,
.empty-state {
  padding: 80px 0;
  text-align: center;
}

.article-header {
  text-align: center;
  margin-bottom: clamp(20px, 4vw, 32px);
}

.chinese-divider {
  max-width: 120px;
  margin: 12px auto;
}

.article-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.meta-sep {
  opacity: 0.3;
}

.article-title {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.03em;
  color: var(--text-color);
  margin-bottom: 16px;
}

@media (max-width: 640px) {
  .article-title {
    font-size: 26px;
  }
}

.article-summary {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.6;
  max-width: 600px;
  margin: 0 0 16px;
  text-align: left;
}

.article-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.article-tag {
  --el-tag-bg-color: var(--bg-secondary);
  --el-tag-border-color: var(--border-color);
  --el-tag-text-color: var(--text-secondary);
  border-radius: 20px;
  font-family: var(--font-body);
}

.article-cover {
  margin-bottom: 32px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.article-cover img {
  width: 100%;
  height: auto;
  display: block;
}

.article-content {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: clamp(20px, 3vw, 32px);
  margin-bottom: 24px;
  font-size: clamp(15px, 1vw + 10px, 17px);
  line-height: 1.75;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  margin-top: 32px;
  margin-bottom: 16px;
}

.article-content :deep(p) {
  margin-bottom: 16px;
}

.md-content {
  overflow-wrap: break-word;
}

.md-content :deep(img) {
  border-radius: var(--radius-sm);
  margin: 24px 0;
  max-width: 100%;
  height: auto;
}

.md-content :deep(video),
.md-content :deep(audio) {
  max-width: 100%;
  display: block;
  margin: 16px 0;
  border-radius: var(--radius-sm);
}

.md-content :deep(table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
}

.md-content :deep(pre),
.md-content :deep(code) {
  max-width: 100%;
  overflow-x: auto;
}

.article-footer {
  padding: 20px 0;
  border-top: 1px solid var(--border-color);
  margin-bottom: 40px;
}

/* Comments */
.comment-section {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 28px;
  margin-bottom: 48px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.comment-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.comment-count {
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 10px;
  border-radius: 20px;
}

.login-hint {
  text-align: center;
  padding: 20px;
  background: var(--bg-color);
  border-radius: var(--radius-sm);
  margin-bottom: 20px;
}

.login-hint .el-button {
  font-family: var(--font-body);
  color: var(--accent-color);
}

.empty-comments {
  text-align: center;
  padding: 32px;
  color: var(--text-secondary);
  font-size: 14px;
}

.comments-list {
  margin-top: 16px;
}
</style>
