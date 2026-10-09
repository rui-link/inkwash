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
    <el-card class="table-container">
      <template #header>
        <div class="flex-x-between">
          <span>{{ $t('group.management') }}</span>
          <el-button type="primary" v-hasPerm="['system:group:create']" @click="handleAdd()">{{
            $t('group.newGroup')
          }}</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column prop="name" :label="$t('group.name')" min-width="200" />
        <el-table-column prop="status" :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="BaseStatusMap[row.status]?.type">{{ BaseStatusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('common.remark')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="$t('common.actions')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link v-hasPerm="['system:group:create']" @click="handleAdd(row)">{{
              $t('group.newSubGroup')
            }}</el-button>
            <el-button type="primary" link v-hasPerm="['system:group:update']" @click="handleEdit(row)">{{
              $t('common.edit')
            }}</el-button>
            <el-button type="success" link v-hasPerm="['system:group:assign-role']" @click="handleAssignRoles(row)">{{
              $t('group.assignRoles')
            }}</el-button>
            <el-button type="danger" link v-hasPerm="['system:group:delete']" @click="handleDelete(row)">{{
              $t('common.delete')
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialog.visible" :title="dialog.title" :width="dialog.width" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item :label="$t('group.name')" prop="name">
          <el-input v-model="formData.name" :placeholder="$t('group.placeholderName')" />
        </el-form-item>
        <el-form-item :label="$t('group.parentGroup')">
          <el-tree-select
            v-model="formData.parentId"
            :data="parentTreeData"
            :props="{ label: 'name', value: 'id' }"
            check-strictly
            :placeholder="$t('group.placeholderParent')"
            clearable />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">{{ $t('common.enable') }}</el-radio>
            <el-radio :value="0">{{ $t('common.disable') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('common.remark')">
          <el-input v-model="formData.remark" type="textarea" :rows="3" :placeholder="$t('group.placeholderRemark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Role Assignment Dialog -->
    <el-dialog v-model="roleDialog.visible" :title="$t('group.assignRoles')" width="500px" destroy-on-close>
      <el-select
        v-model="roleDialog.selectedRoleIds"
        multiple
        :placeholder="$t('group.placeholderRoles')"
        style="width: 100%">
        <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSaveRoles">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { groupApi, roleApi, getErrorMessage } from '@inkwash/share';
import { BaseStatus, BaseStatusMap } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const loading = ref(false);
const treeData = ref([]);
const roleOptions = ref([]);
const submitting = ref(false);

const parentTreeData = computed(() => [{ id: 0, name: t('group.topGroup'), children: treeData.value }]);

const dialog = reactive({
  visible: false,
  type: 'add',
  title: '',
  width: '500px',
});

const roleDialog = reactive({
  visible: false,
  groupId: null,
  selectedRoleIds: [],
});

const formData = reactive({
  id: null,
  name: '',
  parentId: 0,
  status: 1,
  remark: '',
});

const rules = {
  name: [
    {
      required: true,
      message: t('group.errNameRequired'),
      trigger: 'blur',
    },
  ],
};

const formRef = ref(null);

async function fetchData() {
  loading.value = true;
  try {
    treeData.value = await groupApi.getGroupTree();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

async function fetchRoles() {
  try {
    const res = await roleApi.getRoleList({ page: 1, size: 100 });
    roleOptions.value = res.list || [];
  } catch (e) {
    /* ignore */
  }
}

function handleAdd(row) {
  dialog.type = 'add';
  dialog.title = t('group.newGroup');
  dialog.visible = true;
  resetForm();
  if (row) {
    formData.parentId = row.id;
  }
}

function handleEdit(row) {
  dialog.type = 'edit';
  dialog.title = t('group.editGroup');
  dialog.visible = true;
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    parentId: row.parentId || 0,
    status: row.status,
    remark: row.remark || '',
  });
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(t('group.confirmDelete'), t('common.hint'), {
      type: 'warning',
    });
    await groupApi.deleteGroup(row.id);
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
      await groupApi.createGroup(formData);
    } else {
      await groupApi.updateGroup(formData.id, formData);
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

function handleAssignRoles(row) {
  roleDialog.groupId = row.id;
  roleDialog.selectedRoleIds = row.roles?.map((r) => r.id) || [];
  roleDialog.visible = true;
}

async function handleSaveRoles() {
  await groupApi.assignRoles({
    groupId: roleDialog.groupId,
    roleIds: roleDialog.selectedRoleIds,
  });
  ElMessage.success(t('group.rolesAssigned'));
  roleDialog.visible = false;
  fetchData();
}

function resetForm() {
  Object.assign(formData, {
    id: null,
    name: '',
    parentId: 0,
    status: 1,
    remark: '',
  });
}

onMounted(() => {
  fetchData();
  fetchRoles();
});
</script>
