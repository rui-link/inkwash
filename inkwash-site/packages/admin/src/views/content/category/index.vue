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
  <div class="category-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ $t('category.management') }}</span>
          <el-button type="primary" @click="handleCreate">{{ $t('category.newCategory') }}</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="categoryTree"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
        <el-table-column prop="name" :label="$t('category.name')" width="200" />
        <el-table-column prop="slug" :label="$t('category.slug')" width="150" />
        <el-table-column :label="$t('category.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? $t('category.enabled') : $t('category.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('category.remark')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" :label="$t('common.created')" width="180" :formatter="formatDateCell" />
        <el-table-column :label="$t('common.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? $t('category.createCategory') : $t('category.editCategory')"
      width="600px"
      @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$t('category.parentCategory')" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="categoryTreeSelect"
            :props="{ children: 'children', label: 'name', value: 'id' }"
            check-strictly
            :placeholder="$t('category.placeholderParent')"
            clearable />
        </el-form-item>
        <el-form-item :label="$t('category.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('category.slug')" prop="slug">
          <el-input v-model="form.slug" :placeholder="$t('category.placeholderSlug')" />
        </el-form-item>
        <el-form-item :label="$t('category.status')" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('category.remark')" prop="remark">
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
  getCategoriesTree,
  getCategory,
  createCategory,
  updateCategory,
  deleteCategory,
  getErrorMessage,
  formatDateCell,
} from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const categoryTree = ref([]);
const categoryTreeSelect = ref([]);
const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref('create');
const formRef = ref(null);

const form = reactive({
  id: null,
  parentId: null,
  name: '',
  slug: '',
  status: 1,
  remark: '',
});

const rules = computed(() => ({
  name: [{ required: true, message: t('category.name'), trigger: 'blur' }],
}));

const fetchCategories = async () => {
  loading.value = true;
  try {
    const res = await getCategoriesTree();
    categoryTree.value = res.data || res || [];
    buildTreeSelect(categoryTree.value);
  } catch (error) {
    ElMessage.error(t('category.fetchFailed'));
    categoryTree.value = [];
  } finally {
    loading.value = false;
  }
};

const buildTreeSelect = (categories, result = []) => {
  categories.forEach((cat) => {
    const node = { id: cat.id, name: cat.name, children: [] };
    if (cat.children) {
      node.children = buildTreeSelect(cat.children, node.children);
    }
    result.push(node);
  });
  categoryTreeSelect.value = result;
  return result;
};

const handleCreate = () => {
  dialogMode.value = 'create';
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  dialogMode.value = 'edit';
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId || null,
    name: row.name,
    slug: row.slug || '',
    status: row.status ?? 1,
    remark: row.remark || '',
  });
  dialogVisible.value = true;
};

const handleDelete = (row) => {
  const message = row.children?.length
    ? t('category.deleteConfirmWithChildren', { name: row.name })
    : t('category.deleteConfirm', { name: row.name });
  ElMessageBox.confirm(message, t('common.confirm'), { type: 'warning' })
    .then(async () => {
      await deleteCategory(row.id);
      ElMessage.success(t('category.deleteSuccess'));
      fetchCategories();
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
      if (form.parentId) {
        data.parentId = form.parentId;
      }
      if (dialogMode.value === 'create') {
        await createCategory(data);
        ElMessage.success(t('category.createSuccess'));
      } else {
        await updateCategory(form.id, data);
        ElMessage.success(t('category.updateSuccess'));
      }
      dialogVisible.value = false;
      fetchCategories();
    } catch (error) {
      ElMessage.error(getErrorMessage(error, t('category.operationFailed')));
    } finally {
      submitLoading.value = false;
    }
  });
};

const resetForm = () => {
  Object.assign(form, {
    id: null,
    parentId: null,
    name: '',
    slug: '',
    status: 1,
    remark: '',
  });
  formRef.value?.resetFields();
};

onMounted(() => {
  fetchCategories();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
