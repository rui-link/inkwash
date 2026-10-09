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
        <el-form-item :label="$t('sensitive.type')">
          <el-select v-model="queryParams.type" :placeholder="$t('sensitive.placeholderType')" clearable>
            <el-option v-for="opt in typeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-select v-model="queryParams.status" :placeholder="$t('sensitive.placeholderStatus')" clearable>
            <el-option :value="1" :label="$t('common.enable')" />
            <el-option :value="0" :label="$t('common.disable')" />
          </el-select>
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
          <span>{{ $t('sensitive.management') }}</span>
          <el-button type="primary" v-hasPerm="['cms:sensitive:create']" @click="handleAdd">{{
            $t('sensitive.newSensitive')
          }}</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData">
        <el-table-column prop="word" :label="$t('sensitive.word')" min-width="150" />
        <el-table-column prop="type" :label="$t('sensitive.type')" width="100">
          <template #default="{ row }">
            <el-tag>{{ getTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="BaseStatusMap[row.status]?.type">{{ BaseStatusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('common.remark')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="$t('common.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link v-hasPerm="['cms:sensitive:update']" @click="handleEdit(row)">{{
              $t('common.edit')
            }}</el-button>
            <el-button type="danger" link v-hasPerm="['cms:sensitive:delete']" @click="handleDelete(row)">{{
              $t('common.delete')
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination :page="queryParams.page" :limit="queryParams.size" :total="total" @pagination="handlePagination" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" :width="dialog.width" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item :label="$t('sensitive.word')" prop="word">
          <el-input v-model="formData.word" :placeholder="$t('sensitive.placeholderWord')" />
        </el-form-item>
        <el-form-item :label="$t('sensitive.type')">
          <el-select v-model="formData.type" :placeholder="$t('sensitive.placeholderType')">
            <el-option v-for="opt in typeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">{{ $t('common.enable') }}</el-radio>
            <el-radio :value="0">{{ $t('common.disable') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('common.remark')">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            :placeholder="$t('sensitive.placeholderRemark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { sensitiveApi, getErrorMessage } from '@inkwash/share';
import { BaseStatus, BaseStatusMap } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import Pagination from '@/components/Pagination/index.vue';

const { t } = useI18n();

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const submitting = ref(false);

const queryParams = reactive({
  page: 1,
  size: 10,
  type: null,
  status: null,
});

const dialog = reactive({
  visible: false,
  type: 'add',
  title: '',
  width: '500px',
});

const formData = reactive({
  id: null,
  word: '',
  type: 1,
  status: 1,
  remark: '',
});

const rules = computed(() => ({
  word: [
    {
      required: true,
      message: t('sensitive.placeholderWord'),
      trigger: 'blur',
    },
  ],
}));

const formRef = ref(null);

const typeOptions = computed(() => [
  { value: 1, label: t('sensitive.forbidden') },
  { value: 2, label: t('sensitive.replacement') },
  { value: 3, label: t('sensitive.review') },
]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await sensitiveApi.getSensitiveList(queryParams);
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  dialog.type = 'add';
  dialog.title = t('sensitive.newSensitive');
  dialog.visible = true;
  resetForm();
}

function handleEdit(row) {
  dialog.type = 'edit';
  dialog.title = t('sensitive.editSensitive');
  dialog.visible = true;
  Object.assign(formData, {
    id: row.id,
    word: row.word,
    type: row.type,
    status: row.status,
    remark: row.remark || '',
  });
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(t('sensitive.confirmDelete'), t('common.notice'), {
      type: 'warning',
    });
    await sensitiveApi.deleteSensitive(row.id);
    ElMessage.success(t('common.success'));
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (dialog.type === 'add') {
      await sensitiveApi.createSensitive(formData);
    } else {
      await sensitiveApi.updateSensitive(formData.id, formData);
    }
    ElMessage.success(t('common.success'));
    dialog.visible = false;
    fetchData();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    submitting.value = false;
  }
}

function resetForm() {
  Object.assign(formData, {
    id: null,
    word: '',
    type: 1,
    status: 1,
    remark: '',
  });
}

function handleQuery() {
  queryParams.page = 1;
  fetchData();
}

function resetQuery() {
  queryParams.type = null;
  queryParams.status = null;
  queryParams.page = 1;
  fetchData();
}

function handlePagination({ page, limit }) {
  queryParams.page = page;
  queryParams.size = limit;
  fetchData();
}

function getTypeLabel(type) {
  return typeOptions.value.find((o) => o.value === type)?.label || t('common.unknown');
}

onMounted(fetchData);
</script>
