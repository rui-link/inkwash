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
        <el-form-item :label="$t('monitor.username')">
          <el-input v-model="queryParams.username" :placeholder="$t('monitor.placeholderUser')" clearable />
        </el-form-item>
        <el-form-item :label="$t('monitor.module')">
          <el-input v-model="queryParams.module" :placeholder="$t('monitor.placeholderModule')" clearable />
        </el-form-item>
        <el-form-item :label="$t('monitor.operation')">
          <el-input v-model="queryParams.operation" :placeholder="$t('monitor.placeholderOperation')" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">{{ $t('common.search') }}</el-button>
          <el-button @click="resetQuery">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-card class="table-container">
      <template #header>
        <span>{{ $t('monitor.journal') }}</span>
      </template>

      <el-table v-loading="loading" :data="tableData">
        <el-table-column prop="userName" :label="$t('monitor.username')" width="140" />
        <el-table-column prop="module" :label="$t('monitor.module')" width="100" />
        <el-table-column prop="operation" :label="$t('monitor.operation')" min-width="60" show-overflow-tooltip />
        <el-table-column prop="url" :label="$t('monitor.url')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="method" :label="$t('monitor.method')" width="260" />
        <el-table-column prop="ip" :label="$t('monitor.ipAddress')" width="120" />
        <el-table-column prop="duration" :label="$t('monitor.duration')" width="80" />
        <el-table-column prop="result" :label="$t('monitor.result')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 1 ? 'success' : 'danger'">{{
              row.result === 1 ? $t('enum.loginStatus.success') : $t('enum.loginStatus.failed')
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('monitor.operationTime')" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <Pagination :page="queryParams.page" :limit="queryParams.size" :total="total" @pagination="handlePagination" />
    </el-card>
  </div>
</template>

<script setup>
import { monitorApi } from '@inkwash/share';
import { formatDate } from '@inkwash/share';
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import Pagination from '@/components/Pagination/index.vue';

const { t } = useI18n();

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);

const queryParams = reactive({
  page: 1,
  size: 10,
  username: '',
  module: '',
  operation: '',
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await monitorApi.getJournals(queryParams);
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.page = 1;
  fetchData();
}

function resetQuery() {
  queryParams.username = '';
  queryParams.module = '';
  queryParams.operation = '';
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
