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
    <div class="search-container">
      <el-form :inline="true" :model="queryParams">
        <el-form-item :label="$t('article.title')">
          <el-input v-model="queryParams.title" :placeholder="$t('article.placeholderTitle')" clearable />
        </el-form-item>
        <el-form-item :label="$t('article.status')">
          <el-select v-model="queryParams.status" :placeholder="$t('article.placeholderStatus')" clearable>
            <el-option v-for="(item, key) in ArticleStatusMap" :key="key" :label="item.label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('article.category')">
          <el-tree-select
            v-model="queryParams.categoryId"
            :data="categoryOptions"
            :props="{ label: 'name', value: 'id' }"
            check-strictly
            :placeholder="$t('article.placeholderCategory')"
            clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">{{ $t('common.search') }}</el-button>
          <el-button @click="resetQuery">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card class="table-container">
      <template #header>
        <div class="flex-x-between">
          <span>{{ $t('menu.articleManagement') }}</span>
          <el-button type="primary" v-hasPerm="['cms:article:create']" @click="handleAdd">{{
            $t('article.newArticle')
          }}</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData">
        <el-table-column prop="title" :label="$t('article.title')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" :label="$t('article.author')" width="120" show-overflow-tooltip />
        <el-table-column prop="category.name" :label="$t('article.category')" width="140" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('article.status')" width="100">
          <template #default="{ row }">
            <el-tooltip v-if="row.opinion && row.status === 4" :content="row.opinion" placement="top">
              <el-tag :type="ArticleStatusMap[row.status]?.type">{{
                $t('articleStatus.' + (ArticleStatusLabelKey[row.status] || 'statusDraft'))
              }}</el-tag>
            </el-tooltip>
            <el-tag v-else :type="ArticleStatusMap[row.status]?.type">{{
              $t('articleStatus.' + (ArticleStatusLabelKey[row.status] || 'statusDraft'))
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('common.createTime')" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">{{ $t('article.editBtn') }}</el-button>
            <el-button
              type="info"
              link
              v-if="[ArticleStatus.PENDING, ArticleStatus.APPROVED, ArticleStatus.PUBLISHED].includes(row.status)"
              @click="handlePreview(row)"
              >{{ $t('article.previewBtn') }}</el-button
            >
            <el-button
              type="info"
              link
              v-if="row.status === ArticleStatus.DRAFT || row.status === ArticleStatus.REJECTED"
              :disabled="committing"
              @click="handleCommit(row)"
              >{{
                row.status === ArticleStatus.REJECTED ? $t('article.resubmit') : $t('article.commitBtn')
              }}</el-button
            >
            <el-button type="warning" link v-if="row.status === ArticleStatus.PENDING" @click="handleReview(row)">{{
              $t('article.reviewBtn')
            }}</el-button>
            <el-button
              type="success"
              link
              v-hasPerm="['cms:article:publish']"
              v-if="row.status === ArticleStatus.APPROVED"
              @click="handlePublish(row)"
              >{{ $t('article.publishBtn') }}</el-button
            >
            <el-button
              type="warning"
              link
              v-hasPerm="['cms:article:retract']"
              v-if="row.status === ArticleStatus.PUBLISHED"
              @click="handleRetract(row)"
              >{{ $t('article.retractBtn') }}</el-button
            >
            <el-button type="danger" link @click="handleDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="previewVisible" :title="$t('article.preview')" width="800px" top="5vh">
        <div v-if="previewArticle" v-loading="previewLoading" class="preview-content">
          <h2 class="preview-title">{{ previewArticle.title }}</h2>
          <div class="preview-meta">
            <span>{{ $t('article.authorLabel') }}{{ previewArticle.author }}</span>
            <span>{{ $t('article.categoryLabel') }}{{ previewArticle.category?.name }}</span>
            <span
              >{{ $t('article.statusLabel')
              }}<el-tag :type="ArticleStatusMap[previewArticle.status]?.type" size="small">{{
                $t('articleStatus.' + (ArticleStatusLabelKey[previewArticle.status] || 'statusDraft'))
              }}</el-tag></span
            >
          </div>
          <el-divider />
          <div v-if="previewArticle.opinion && previewArticle.status === 4" class="preview-opinion">
            <strong>{{ $t('article.rejectOpinion') }}</strong>
            <p>{{ previewArticle.opinion }}</p>
          </div>
          <MarkdownPreview :content="previewArticle.content || ''" />
        </div>
      </el-dialog>

      <el-dialog
        v-model="rejectVisible"
        :title="$t('article.rejectDialogTitle')"
        width="520px"
        :close-on-click-modal="false"
        @closed="rejectForm.opinion = ''">
        <div v-if="rejectArticle" class="reject-dialog-body">
          <p class="reject-article-title">{{ rejectArticle.title }}</p>
          <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-position="top">
            <el-form-item :label="$t('article.rejectOpinionLabel')" prop="opinion">
              <el-input
                v-model="rejectForm.opinion"
                type="textarea"
                :rows="4"
                :placeholder="$t('article.rejectOpinionPlaceholder')"
                maxlength="500"
                show-word-limit />
            </el-form-item>
          </el-form>
        </div>
        <template #footer>
          <el-button @click="rejectVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="danger" :loading="rejecting" @click="handleRejectConfirm">
            {{ $t('article.rejectConfirmBtn') }}
          </el-button>
        </template>
      </el-dialog>

      <Pagination :page="queryParams.page" :limit="queryParams.size" :total="total" @pagination="handlePagination" />
    </el-card>
  </div>
</template>
<script setup>
import { articleApi, categoryApi, getErrorMessage } from '@inkwash/share';
import { ArticleStatus, ArticleStatusMap, ArticleStatusLabelKey } from '@inkwash/share';
import { formatDate } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import MarkdownPreview from '@/components/MarkdownPreview.vue';
import Pagination from '@/components/Pagination/index.vue';

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const committing = ref(false);
const tableData = ref([]);
const total = ref(0);
const categoryOptions = ref([]);
const previewVisible = ref(false);
const previewLoading = ref(false);
const previewArticle = ref(null);

const rejectVisible = ref(false);
const rejectArticle = ref(null);
const rejecting = ref(false);
const rejectFormRef = ref(null);
const rejectForm = reactive({ opinion: '' });
const rejectRules = computed(() => ({
  opinion: [
    {
      required: true,
      message: t('article.rejectOpinionRequired'),
      trigger: 'blur',
    },
  ],
}));

const queryParams = reactive({
  page: 1,
  size: 10,
  title: '',
  status: null,
  categoryId: null,
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await articleApi.listAllArticles(queryParams);
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

async function fetchCategories() {
  try {
    categoryOptions.value = await categoryApi.getCategoryTree();
  } catch (e) {
    /* ignore */
  }
}

function handleAdd() {
  router.push('/cms/article/edit');
}

function handleEdit(row) {
  router.push({ path: '/cms/article/edit', query: { id: row.id } });
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(t('article.confirmDelete'), t('common.notice'), {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await articleApi.deleteArticle(row.id);
    ElMessage.success(t('article.deleted'));
    fetchData();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleCommit(row) {
  if (committing.value) return;
  committing.value = true;
  try {
    await ElMessageBox.confirm(t('article.confirmCommit'), t('common.notice'), {
      type: 'info',
    });
  } catch {
    committing.value = false;
    return;
  }
  try {
    if (row.status === ArticleStatus.REJECTED) {
      await articleApi.resubmitArticle(row.id);
      ElMessage.success(t('article.resubmitSuccess'));
    } else {
      await articleApi.commitArticle(row.id);
      ElMessage.success(t('article.committed'));
    }
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    committing.value = false;
    fetchData();
  }
}

function handleReview(row) {
  router.push({ path: '/cms/article/review', query: { id: row.id } });
}

function openRejectDialog(row) {
  rejectArticle.value = row;
  rejectForm.opinion = '';
  rejectVisible.value = true;
}

async function handleRejectConfirm() {
  const valid = await rejectFormRef.value.validate().catch(() => false);
  if (!valid) return;
  rejecting.value = true;
  try {
    await articleApi.reviewArticle(rejectArticle.value.id, {
      approved: false,
      opinion: rejectForm.opinion,
    });
    ElMessage.success(t('article.rejected'));
    rejectVisible.value = false;
    fetchData();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    rejecting.value = false;
  }
}

async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(t('article.confirmPublish'), t('common.notice'), {
      type: 'info',
    });
  } catch {
    return;
  }
  try {
    await articleApi.publishArticle(row.id);
    ElMessage.success(t('article.published'));
    fetchData();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleRetract(row) {
  try {
    await ElMessageBox.confirm(t('article.confirmRetract'), t('common.notice'), {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await articleApi.retractArticle(row.id);
    ElMessage.success(t('article.retracted'));
    fetchData();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handlePreview(row) {
  previewLoading.value = true;
  try {
    previewArticle.value = await articleApi.getArticle(row.id);
    previewVisible.value = true;
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    previewLoading.value = false;
  }
}

function handleQuery() {
  queryParams.page = 1;
  fetchData();
}

function resetQuery() {
  queryParams.title = '';
  queryParams.status = null;
  queryParams.categoryId = null;
  queryParams.page = 1;
  fetchData();
}

function handlePagination({ page, limit }) {
  queryParams.page = page;
  queryParams.size = limit;
  fetchData();
}

onMounted(() => {
  fetchData();
  fetchCategories();
});
</script>

<style scoped>
.preview-content {
  padding: 0 8px;
}
.preview-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 12px;
}
.preview-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.preview-opinion {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.preview-opinion strong {
  color: #f56c6c;
}
.preview-opinion p {
  margin: 4px 0 0;
  color: var(--el-text-color-primary);
}
.reject-dialog-body {
  padding: 0 4px;
}
.reject-article-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}
</style>
