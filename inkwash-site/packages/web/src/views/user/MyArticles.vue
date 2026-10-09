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
  <div class="my-articles">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ $t('myArticles.title') }}</h2>
        <p class="page-desc">{{ $t('myArticles.desc') }}</p>
      </div>
      <el-button type="primary" @click="$router.push({ name: 'ArticleNew' })">
        {{ $t('myArticles.writeArticle') }}
      </el-button>
    </div>

    <div class="table-card">
      <el-table :data="articles" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" :label="$t('article.title')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="$t('common.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" class="status-tag">{{
              $t(statusLabel(row.status))
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="articles.some((r) => r.opinion)"
          :label="$t('article.rejectOpinion')"
          min-width="200"
          show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.opinion" class="reject-opinion">{{ row.opinion }}</span>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('myArticles.stats')" width="180">
          <template #default="{ row }">
            <span class="stat-item">
              <AppIcon name="thumbUp" :size="14" />
              {{ row.tally?.agreeCount || 0 }}
            </span>
            <span class="stat-item">
              <AppIcon name="comment" :size="14" />
              {{ row.tally?.commentCount || 0 }}
            </span>
            <span class="stat-item">
              <AppIcon name="eye" :size="14" />
              {{ row.tally?.viewCount || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('myArticles.created')" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="200">
          <template #default="{ row }">
            <el-button text size="small" @click="editArticle(row)">{{ $t('myArticles.edit') }}</el-button>
            <el-button
              v-if="row.status === 4"
              text
              size="small"
              type="warning"
              :loading="resubmitting && resubmittingId === row.id"
              @click="resubmitArticle(row)">
              {{ $t('myArticles.resubmit') }}
            </el-button>
            <el-button text size="small" type="primary" @click="$router.push(`/article/${row.id}`)">{{
              $t('myArticles.view')
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { getMyArticles, resubmitArticle as apiResubmitArticle, getErrorMessage, formatDate } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';
import { statusLabel, statusType } from '@/utils/helpers';

const router = useRouter();
const { t } = useI18n();
const articles = ref([]);
const loading = ref(false);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const resubmitting = ref(false);
const resubmittingId = ref(null);

onMounted(() => fetchData());

async function fetchData() {
  loading.value = true;
  try {
    const res = await getMyArticles({
      page: currentPage.value,
      size: pageSize.value,
    });
    articles.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.failed')));
  } finally {
    loading.value = false;
  }
}

function editArticle(row) {
  if (row.status === 1 || row.status === 4) {
    router.push({ name: 'ArticleEdit', params: { id: row.id } });
  } else {
    ElMessage.info(t('myArticles.editNotAllowed'));
  }
}

/**
 * Resubmit a rejected article.
 *
 * No dialog: the author is expected to act on the rejection opinion first — it already
 * has its own column in the table above — and the endpoint takes no note (D-15). The
 * previous dialog collected a note that the backend silently discarded.
 */
async function resubmitArticle(row) {
  resubmitting.value = true;
  resubmittingId.value = row.id;
  try {
    await apiResubmitArticle(row.id);
    ElMessage.success(t('myArticles.resubmitSuccess'));
    fetchData();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.failed')));
  } finally {
    resubmitting.value = false;
    resubmittingId.value = null;
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
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

.table-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 20px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-item svg {
  opacity: 0.5;
}

.status-tag {
  font-family: var(--font-body);
  font-weight: 500;
  border-radius: 12px;
}

.reject-opinion {
  color: var(--color-danger, #f56c6c);
  font-size: 13px;
}

.text-secondary {
  color: var(--text-secondary);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}
</style>
