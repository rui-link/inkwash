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
        <el-form-item :label="$t('monitor.loginAccount')">
          <el-input v-model="queryParams.identity" :placeholder="$t('monitor.placeholderIdentity')" clearable />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-select v-model="queryParams.status" :placeholder="$t('common.pleaseSelect')" clearable>
            <el-option :value="1" :label="$t('enum.loginStatus.success')" />
            <el-option :value="0" :label="$t('enum.loginStatus.failed')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('monitor.loginTime')">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            :range-separator="$t('monitor.dateSeparator')"
            :start-placeholder="$t('monitor.startTime')"
            :end-placeholder="$t('monitor.endTime')"
            value-format="YYYY-MM-DD" />
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
          <span>{{ $t('monitor.loginLog') }}</span>
          <div>
            <el-button
              type="danger"
              v-hasPerm="['monitor:login:batch-delete']"
              :disabled="!selectedIds.length"
              @click="handleBatchDelete"
              >{{ $t('monitor.batchDelete') }}</el-button
            >
            <el-button type="danger" v-hasPerm="['monitor:login:clear']" @click="handleClearAll">{{
              $t('monitor.clearAll')
            }}</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="identity" :label="$t('monitor.loginAccount')" min-width="150" />
        <el-table-column prop="loginType" :label="$t('monitor.loginType')" width="100">
          <template #default="{ row }">
            <el-tag :type="AuthTypeMap[row.loginType]?.type">{{
              AuthTypeMap[row.loginType]?.label || row.loginType
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" :label="$t('monitor.ipAddress')" width="140" />
        <el-table-column prop="device" :label="$t('monitor.device')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="browser" :label="$t('monitor.browser')" width="120" show-overflow-tooltip />
        <el-table-column prop="ostype" :label="$t('monitor.os')" width="120" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('common.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{
              row.status === 1 ? $t('enum.loginStatus.success') : $t('enum.loginStatus.failed')
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTime" :label="$t('monitor.loginTime')" width="170">
          <template #default="{ row }">{{ formatDate(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link v-hasPerm="['monitor:login:delete']" @click="handleDelete(row)">{{
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
import { monitorApi, getErrorMessage, AuthTypeMap } from '@inkwash/share';
import { formatDate } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import Pagination from '@/components/Pagination/index.vue';

const { t } = useI18n();

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const selectedIds = ref([]);

const queryParams = reactive({
  page: 1,
  size: 10,
  identity: '',
  status: null,
  startTime: '',
  endTime: '',
});

const dateRange = ref([]);

async function fetchData() {
  loading.value = true;
  try {
    const params = { ...queryParams };
    if (dateRange.value?.length === 2) {
      params.startTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const res = await monitorApi.getLoginLogs(params);
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
    await ElMessageBox.confirm(t('monitor.confirmDeleteLog'), t('common.tip'), {
      type: 'warning',
    });
    await monitorApi.deleteLoginLog(row.id);
    ElMessage.success(t('common.deleteSuccess'));
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) {
    ElMessage.warning(t('monitor.selectToDelete'));
    return;
  }
  try {
    await ElMessageBox.confirm(
      t('monitor.batchDeleteConfirm', {
        count: selectedIds.value.length,
      }),
      t('common.tip'),
      {
        type: 'warning',
      },
    );
    await monitorApi.batchDeleteLoginLogs(selectedIds.value);
    ElMessage.success(t('monitor.batchDeleted'));
    selectedIds.value = [];
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleClearAll() {
  try {
    await ElMessageBox.confirm(t('monitor.clearAllConfirm'), t('common.warning'), {
      type: 'error',
    });
    await monitorApi.clearLoginLogs();
    ElMessage.success(t('monitor.cleared'));
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id);
}

function handleQuery() {
  queryParams.page = 1;
  fetchData();
}

function resetQuery() {
  queryParams.identity = '';
  queryParams.status = null;
  dateRange.value = [];
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
