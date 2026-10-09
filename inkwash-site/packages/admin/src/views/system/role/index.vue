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
        <el-form-item :label="$t('role.name')">
          <el-input v-model="queryParams.name" :placeholder="$t('role.placeholderName')" clearable />
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
          <span>{{ $t('role.management') }}</span>
          <el-button type="primary" v-hasPerm="['system:role:create']" @click="handleAdd">{{
            $t('role.newRole')
          }}</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData">
        <el-table-column prop="name" :label="$t('role.name')" min-width="150" />
        <el-table-column prop="code" :label="$t('role.code')" width="150" />
        <el-table-column prop="status" :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="BaseStatusMap[row.status]?.type">{{ BaseStatusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('common.remark')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="$t('common.actions')" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link v-hasPerm="['system:role:update']" @click="handleEdit(row)">{{
              $t('common.edit')
            }}</el-button>
            <el-button
              type="success"
              link
              v-hasPerm="['system:role:assign-permission']"
              @click="handleAssignPermissions(row)"
              >{{ $t('role.assignPermissions') }}</el-button
            >
            <el-button type="danger" link v-hasPerm="['system:role:delete']" @click="handleDelete(row)">{{
              $t('common.delete')
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination :page="queryParams.page" :limit="queryParams.size" :total="total" @pagination="handlePagination" />
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" :width="dialog.width" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item :label="$t('role.name')" prop="name">
          <el-input v-model="formData.name" :placeholder="$t('role.placeholderName')" />
        </el-form-item>
        <el-form-item :label="$t('role.code')" prop="code">
          <el-input v-model="formData.code" :placeholder="$t('role.placeholderCode')" />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">{{ $t('common.enable') }}</el-radio>
            <el-radio :value="0">{{ $t('common.disable') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('common.remark')">
          <el-input v-model="formData.remark" type="textarea" :rows="3" :placeholder="$t('role.placeholderRemark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Permission Assignment Dialog -->
    <el-dialog v-model="permDialog.visible" :title="$t('role.assignPermissions')" width="500px" destroy-on-close>
      <el-tree
        ref="permTreeRef"
        :data="permissionTree"
        :props="{ label: 'name', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="permDialog.checkedKeys" />
      <template #footer>
        <el-button @click="permDialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSavePermissions">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { roleApi, permissionApi, getErrorMessage } from '@inkwash/share';
import { BaseStatus, BaseStatusMap } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import Pagination from '@/components/Pagination/index.vue';

const { t } = useI18n();

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const permissionTree = ref([]);
const permTreeRef = ref(null);
const submitting = ref(false);

const queryParams = reactive({
  page: 1,
  size: 10,
  name: '',
});

const dialog = reactive({
  visible: false,
  type: 'add',
  title: '',
  width: '500px',
});

const permDialog = reactive({
  visible: false,
  roleId: null,
  checkedKeys: [],
});

const formData = reactive({
  id: null,
  name: '',
  code: '',
  status: 1,
  remark: '',
});

const rules = {
  name: [
    {
      required: true,
      message: t('role.errNameRequired'),
      trigger: 'blur',
    },
  ],
  code: [
    {
      required: true,
      message: t('role.errCodeRequired'),
      trigger: 'blur',
    },
  ],
};

const formRef = ref(null);

async function fetchData() {
  loading.value = true;
  try {
    const res = await roleApi.getRoleList(queryParams);
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
}

async function fetchPermissions() {
  try {
    const res = await permissionApi.getPermissionList({
      page: 1,
      size: 1000,
    });
    permissionTree.value = res.list || [];
  } catch (e) {
    /* ignore */
  }
}

function handleAdd() {
  dialog.type = 'add';
  dialog.title = t('role.newRole');
  dialog.visible = true;
  resetForm();
}

function handleEdit(row) {
  dialog.type = 'edit';
  dialog.title = t('role.editRole');
  dialog.visible = true;
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    code: row.code,
    status: row.status,
    remark: row.remark || '',
  });
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(t('role.confirmDelete'), t('common.hint'), {
      type: 'warning',
    });
    await roleApi.deleteRole(row.id);
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
    const codeRaw = (formData.code || '').trim();
    const code = codeRaw.toUpperCase().startsWith('ROLE_') ? codeRaw : `ROLE_${codeRaw}`;
    const payload = { ...formData, code };
    if (dialog.type === 'add') {
      await roleApi.createRole(payload);
    } else {
      await roleApi.updateRole(formData.id, payload);
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

async function handleAssignPermissions(row) {
  permDialog.roleId = row.id;
  permDialog.checkedKeys = [];
  await fetchPermissions();
  try {
    const detail = await roleApi.getRoleDetail(row.id);
    permDialog.checkedKeys = detail.permissions?.map((p) => p.id) || [];
  } catch (e) {
    permDialog.checkedKeys = row.permissions?.map((p) => p.id) || [];
  }
  permDialog.visible = true;
}

async function handleSavePermissions() {
  const checkedNodes = permTreeRef.value?.getCheckedKeys() || [];
  const halfCheckedNodes = permTreeRef.value?.getHalfCheckedKeys() || [];
  await roleApi.assignPermissions({
    roleId: permDialog.roleId,
    permIds: [...checkedNodes, ...halfCheckedNodes],
  });
  ElMessage.success(t('role.permsAssigned'));
  permDialog.visible = false;
  fetchData();
}

function resetForm() {
  Object.assign(formData, {
    id: null,
    name: '',
    code: '',
    status: 1,
    remark: '',
  });
}

function handleQuery() {
  queryParams.page = 1;
  fetchData();
}

function resetQuery() {
  queryParams.name = '';
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
});
</script>
