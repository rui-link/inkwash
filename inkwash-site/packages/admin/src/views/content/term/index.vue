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
  <div class="tag-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('term.management') }}</span>
          <el-button type="primary" @click="handleCreate">{{ $t('term.newTag') }}</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item :label="$t('term.name')">
          <el-input v-model="searchForm.name" :placeholder="$t('term.name')" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ $t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tagList" row-key="id">
        <el-table-column prop="id" :label="$t('common.id')" width="80" />
        <el-table-column prop="name" :label="$t('term.name')" />
        <el-table-column prop="slug" :label="$t('term.slug')" width="150" />
        <el-table-column :label="$t('term.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="BaseStatusMap[row.status]?.type">{{ BaseStatusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('term.remark')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" :label="$t('common.created')" width="180" :formatter="formatDateCell" />
        <el-table-column :label="$t('common.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchTags"
        @current-change="fetchTags"
        style="margin-top: 20px; justify-content: flex-end" />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? $t('term.createTag') : $t('term.editTag')"
      width="600px"
      @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$t('term.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('term.slug')" prop="slug">
          <el-input v-model="form.slug" :placeholder="$t('term.placeholderSlug')" />
        </el-form-item>
        <el-form-item :label="$t('term.status')" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('term.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {
  getTags,
  getTag,
  createTag,
  updateTag,
  deleteTag,
  getErrorMessage,
  formatDateCell,
  BaseStatusMap,
} from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const tagList = ref([]);
const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref('create');
const formRef = ref(null);

const searchForm = reactive({
  name: '',
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const form = reactive({
  id: null,
  name: '',
  slug: '',
  status: 1,
  remark: '',
});

const rules = {
  name: [{ required: true, message: t('term.name'), trigger: 'blur' }],
};

const fetchTags = async () => {
  loading.value = true;
  try {
    const res = await getTags({
      page: pagination.page,
      size: pagination.size,
      name: searchForm.name,
    });
    tagList.value = res.list || res.content || res.records || res.data?.records || [];
    pagination.total = res.total || res.totalElements || 0;
  } catch (error) {
    ElMessage.error(t('term.fetchFailed'));
    tagList.value = [];
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  fetchTags();
};

const handleReset = () => {
  searchForm.name = '';
  pagination.page = 1;
  fetchTags();
};

const handleCreate = () => {
  dialogMode.value = 'create';
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  dialogMode.value = 'edit';
  getTag(row.id)
    .then((res) => {
      const data = res.data || res;
      Object.assign(form, {
        id: data.id,
        name: data.name,
        slug: data.slug || '',
        status: data.status,
        remark: data.remark,
      });
      dialogVisible.value = true;
    })
    .catch(() => {
      ElMessage.error(t('term.fetchFailed'));
    });
};

const handleDelete = (row) => {
  ElMessageBox.confirm(t('term.deleteConfirm', { name: row.name }), t('common.confirm'), {
    type: 'warning',
  })
    .then(async () => {
      await deleteTag(row.id);
      ElMessage.success(t('term.deleteSuccess'));
      fetchTags();
    })
    .catch(() => {});
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      const data = {
        name: form.name,
        slug: form.slug,
        status: form.status,
        remark: form.remark,
      };
      if (dialogMode.value === 'create') {
        await createTag(data);
        ElMessage.success(t('term.createSuccess'));
      } else {
        await updateTag(form.id, data);
        ElMessage.success(t('term.updateSuccess'));
      }
      dialogVisible.value = false;
      fetchTags();
    } catch (error) {
      ElMessage.error(getErrorMessage(error, t('term.operationFailed')));
    } finally {
      submitLoading.value = false;
    }
  });
};

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    slug: '',
    status: 1,
    remark: '',
  });
  formRef.value?.resetFields();
};

onMounted(() => {
  fetchTags();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 20px;
}
</style>
