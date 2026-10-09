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
      <el-form :inline="true">
        <el-form-item :label="$t('comment.content')">
          <el-input v-model="searchContent" :placeholder="$t('comment.content')" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">{{ $t('common.search') }}</el-button>
          <el-button :icon="Refresh" @click="handleReset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card shadow="never" class="table-container">
      <template #header>
        <div class="card-header">
          <span>{{ $t('comment.management') }}</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="content" :label="$t('comment.content')" show-overflow-tooltip min-width="240" />
        <el-table-column prop="nickname" :label="$t('comment.author')" width="120" show-overflow-tooltip />
        <el-table-column prop="articleTitle" :label="$t('comment.articleTitle')" width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" :label="$t('comment.commentTime')" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">{{
              $t('common.delete')
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination :page="queryParams.page" :limit="queryParams.size" :total="total" @pagination="handlePagination" />
    </el-card>
  </div>
</template>
<script setup>
import { Delete, Search, Refresh } from '@element-plus/icons-vue';
import { articleApi, getErrorMessage } from '@inkwash/share';
import { formatDate } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import Pagination from '@/components/Pagination/index.vue';

const { t } = useI18n();

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const searchContent = ref('');

const queryParams = reactive({
  page: 1,
  size: 10,
  content: '',
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await articleApi.listAllComments(queryParams);
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(t('comment.confirmDelete'), t('common.hint'), {
      type: 'warning',
    });
    await articleApi.deleteComment(row.id);
    ElMessage.success(t('common.success'));
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

function handleQuery() {
  queryParams.page = 1;
  queryParams.content = searchContent.value;
  fetchData();
}

function handleReset() {
  searchContent.value = '';
  queryParams.content = '';
  queryParams.page = 1;
  fetchData();
}

function handlePagination({ page, limit }) {
  queryParams.page = page;
  queryParams.size = limit;
  fetchData();
}

onMounted(fetchData);
</script>

<style scoped>
.app-container {
  padding: 15px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
