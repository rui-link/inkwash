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
  <div class="app-container" v-loading="loading">
    <el-card v-if="article">
      <template #header>
        <div class="flex-x-between">
          <span>{{ $t('article.publishTitle') }}</span>
          <el-button @click="router.push('/cms/article')">{{ $t('common.back') }}</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="$t('article.title')">{{ article.title }}</el-descriptions-item>
        <el-descriptions-item :label="$t('article.author')">{{ article.authorName }}</el-descriptions-item>
        <el-descriptions-item :label="$t('article.category')">{{ article.categoryName }}</el-descriptions-item>
        <el-descriptions-item :label="$t('article.status')">
          <el-tag :type="ArticleStatusMap[article.status]?.type">{{
            $t('articleStatus.' + (ArticleStatusLabelKey[article.status] || 'statusDraft'))
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('article.reviewOpinion')">{{
          article.opinion || $t('common.none')
        }}</el-descriptions-item>
        <el-descriptions-item :label="$t('article.summary')" :span="2">{{ article.summary }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />
      <div style="text-align: center">
        <el-button type="primary" size="large" :loading="publishing" @click="handlePublish">{{
          $t('article.publishBtn')
        }}</el-button>
        <el-button size="large" @click="router.push('/cms/article')">{{ $t('common.cancel') }}</el-button>
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { articleApi, getErrorMessage } from '@inkwash/share';
import { ArticleStatusMap, ArticleStatusLabelKey } from '@inkwash/share';
import { formatDate } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const loading = ref(true);
const article = ref(null);
const publishing = ref(false);

async function fetchArticle() {
  loading.value = true;
  try {
    article.value = await articleApi.getArticle(route.query.id);
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

async function handlePublish() {
  try {
    await ElMessageBox.confirm(t('article.confirmPublish'), t('common.hint'), {
      type: 'info',
    });
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
    return;
  }
  publishing.value = true;
  try {
    await articleApi.publishArticle(route.query.id);
    ElMessage.success(t('article.published'));
    router.push('/cms/article');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    publishing.value = false;
  }
}

onMounted(fetchArticle);
</script>
