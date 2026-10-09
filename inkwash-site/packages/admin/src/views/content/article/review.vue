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
  <div class="app-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="flex-x-between">
          <span class="text-lg font-bold">{{ $t('article.reviewTitle') }}</span>
          <el-button @click="router.push('/cms/article')">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>

      <template v-if="article">
        <div class="article-preview">
          <h2 class="article-title">{{ article.title }}</h2>
          <div class="article-meta">
            <span>{{ $t('article.authorLabel') }}{{ article.author }}</span>
            <span>{{ $t('article.categoryLabel') }}{{ article.category?.name }}</span>
            <span
              >{{ $t('article.statusLabel')
              }}<el-tag :type="ArticleStatusMap[article.status]?.type" size="small">{{
                $t('articleStatus.' + (ArticleStatusLabelKey[article.status] || 'statusDraft'))
              }}</el-tag></span
            >
            <span v-if="article.createTime">{{ $t('common.createTime') }}{{ formatDate(article.createTime) }}</span>
          </div>
          <div v-if="article.summary" class="article-summary">
            <strong>{{ $t('article.summary') }}：</strong>{{ article.summary }}
          </div>
          <el-divider />
          <MarkdownPreview :content="article.content || ''" />
        </div>

        <el-divider v-if="article.status === ArticleStatus.PENDING" />

        <div v-if="article.status === ArticleStatus.PENDING" class="review-panel">
          <div class="review-choice">
            <span class="review-label">{{ $t('article.reviewDecision') }}</span>
            <el-radio-group v-model="reviewResult">
              <el-radio :value="true">{{ $t('article.approveBtn') }}</el-radio>
              <el-radio :value="false">{{ $t('article.rejectBtn') }}</el-radio>
            </el-radio-group>
          </div>

          <transition name="fade">
            <div v-if="reviewResult === false" class="reject-reason">
              <el-input
                v-model="rejectOpinion"
                type="textarea"
                :rows="4"
                :placeholder="$t('article.rejectOpinionPlaceholder')"
                maxlength="500"
                show-word-limit />
            </div>
          </transition>

          <div class="review-actions">
            <el-button @click="router.push('/cms/article')">{{ $t('common.cancel') }}</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ $t('common.confirm') }}
            </el-button>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import {
  articleApi,
  ArticleStatus,
  ArticleStatusMap,
  ArticleStatusLabelKey,
  getErrorMessage,
  formatDate,
} from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import MarkdownPreview from '@/components/MarkdownPreview.vue';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const loading = ref(false);
const article = ref(null);
const submitting = ref(false);
const reviewResult = ref(null);
const rejectOpinion = ref('');

onMounted(async () => {
  const id = route.query.id;
  if (!id) {
    router.push('/cms/article');
    return;
  }
  loading.value = true;
  try {
    article.value = await articleApi.getArticle(id);
  } catch {
    ElMessage.error(t('article.fetchFailed'));
    router.push('/cms/article');
  } finally {
    loading.value = false;
  }
});

async function handleSubmit() {
  if (reviewResult.value === null) {
    ElMessage.warning(t('article.reviewDecisionRequired'));
    return;
  }
  if (reviewResult.value === false && !rejectOpinion.value.trim()) {
    ElMessage.warning(t('article.rejectOpinionRequired'));
    return;
  }
  submitting.value = true;
  try {
    await articleApi.reviewArticle(article.value.id, {
      approved: reviewResult.value,
      opinion: reviewResult.value ? '' : rejectOpinion.value,
    });
    ElMessage.success(reviewResult.value ? t('article.approved') : t('article.rejected'));
    router.push('/cms/article');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.article-preview {
  padding: 0 8px;
}
.article-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 12px;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
}
.article-summary {
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 16px;
}
.review-panel {
  padding: 0 8px;
}
.review-choice {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.review-label {
  font-weight: 500;
  white-space: nowrap;
}
.reject-reason {
  margin-bottom: 16px;
}
.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
