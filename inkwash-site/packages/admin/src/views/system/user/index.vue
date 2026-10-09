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
        <el-form-item :label="$t('user.nickname')">
          <el-input v-model="queryParams.nickname" :placeholder="$t('user.placeholderNickname')" clearable />
        </el-form-item>
        <el-form-item :label="$t('user.phone')">
          <el-input v-model="queryParams.phone" :placeholder="$t('user.placeholderPhone')" clearable />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-select v-model="queryParams.status" :placeholder="$t('article.placeholderStatus')" clearable>
            <el-option v-for="(item, key) in UserStatusMap" :key="key" :label="item.label" :value="Number(key)" />
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
          <span>{{ $t('user.management') }}</span>
          <el-button type="primary" v-hasPerm="['system:user:create']" @click="handleAdd">{{
            $t('user.newUser')
          }}</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData">
        <el-table-column prop="nickname" :label="$t('user.nickname')" min-width="120" />
        <el-table-column prop="realname" :label="$t('user.realname')" width="120" />
        <el-table-column prop="phone" :label="$t('user.phone')" width="130" />
        <el-table-column prop="email" :label="$t('user.email')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="gender" :label="$t('user.gender')" width="80">
          <template #default="{ row }">{{ GenderMap[row.gender]?.label || $t('enum.gender.unknown') }}</template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="UserStatusMap[row.status]?.type">{{ UserStatusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('common.createTime')" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link v-hasPerm="['system:user:update']" @click="handleEdit(row)">{{
              $t('common.edit')
            }}</el-button>
            <el-button
              type="warning"
              link
              v-hasPerm="['system:user:reset-password']"
              @click="handleResetPassword(row)"
              >{{ $t('user.resetPassword') }}</el-button
            >
            <el-button type="info" link v-hasPerm="['system:user:update-status']" @click="handleStatusChange(row)">
              {{ row.status === UserStatus.ENABLE ? $t('common.disable') : $t('common.enable') }}
            </el-button>
            <el-button type="danger" link v-hasPerm="['system:user:delete']" @click="handleDelete(row)">{{
              $t('common.delete')
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination :page="queryParams.page" :limit="queryParams.size" :total="total" @pagination="handlePagination" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.title" :width="dialog.width" destroy-on-close>
      <div class="dialog-body">
        <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
          <el-form-item :label="$t('user.username')" prop="username" v-if="dialog.type === 'add'">
            <el-input v-model="formData.username" :placeholder="$t('user.placeholderUsername')" />
          </el-form-item>
          <el-form-item :label="$t('user.password')" prop="password" v-if="dialog.type === 'add'">
            <el-input
              v-model="formData.password"
              type="password"
              show-password
              :placeholder="$t('user.placeholderPassword')" />
          </el-form-item>
          <el-form-item :label="$t('user.nickname')" prop="nickname">
            <el-input v-model="formData.nickname" :placeholder="$t('user.placeholderNickname')" />
          </el-form-item>
          <el-form-item :label="$t('user.realname')" prop="realname">
            <el-input
              v-model="formData.realname"
              :placeholder="
                dialog.type === 'edit' ? $t('user.placeholderRealnameEdit') : $t('user.placeholderRealname')
              " />
          </el-form-item>
          <el-form-item :label="$t('user.gender')">
            <el-select v-model="formData.gender" :placeholder="$t('account.placeholderSelect')">
              <el-option v-for="(item, key) in GenderMap" :key="key" :label="item.label" :value="Number(key)" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('user.phone')" prop="phone">
            <el-input
              v-model="formData.phone"
              :disabled="dialog.type === 'edit'"
              :placeholder="$t('user.placeholderPhone')" />
            <span v-if="dialog.type === 'edit'" class="self-managed-hint">{{ $t('user.selfManagedHint') }}</span>
          </el-form-item>
          <el-form-item :label="$t('user.email')" prop="email">
            <el-input
              v-model="formData.email"
              :disabled="dialog.type === 'edit'"
              :placeholder="$t('user.placeholderEmail')" />
            <span v-if="dialog.type === 'edit'" class="self-managed-hint">{{ $t('user.selfManagedHint') }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.status')">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">{{ $t('common.enable') }}</el-radio>
              <el-radio :value="0">{{ $t('common.disable') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="$t('user.groups')">
            <GroupSelect v-model="formData.groupIds" :data="groupOptions" />
          </el-form-item>
          <el-form-item :label="$t('account.motto')">
            <el-input v-model="formData.motto" :placeholder="$t('user.placeholderMotto')" />
          </el-form-item>
          <el-form-item :label="$t('account.location')">
            <el-cascader
              v-model="formData.location"
              :options="regionOptions"
              :props="regionCascaderProps"
              clearable
              :placeholder="$t('user.placeholderLocation')"
              style="width: 100%" />
          </el-form-item>
          <el-form-item :label="$t('account.biography')">
            <el-input
              v-model="formData.biography"
              type="textarea"
              :rows="3"
              :placeholder="$t('user.placeholderBiography')" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { userApi, groupApi, getErrorMessage } from '@inkwash/share';
import { UserStatus, UserStatusMap, Gender, GenderMap, regionOptions } from '@inkwash/share';
import { formatDate } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import Pagination from '@/components/Pagination/index.vue';

import GroupSelect from './components/group-select.vue';

const { t } = useI18n();

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const groupOptions = ref([]);
const submitting = ref(false);
const regionCascaderProps = { checkStrictly: true, emitPath: false };

const queryParams = reactive({
  page: 1,
  size: 10,
  nickname: '',
  phone: '',
  status: null,
});

const dialog = reactive({
  visible: false,
  type: 'add',
  title: '',
  width: '600px',
});

const formData = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  realname: '',
  gender: 0,
  phone: '',
  email: '',
  status: 1,
  motto: '',
  birthDate: '',
  education: null,
  location: '',
  biography: '',
  groupIds: [],
});

const rules = computed(() => ({
  nickname: [
    {
      required: true,
      message: t('user.errNicknameRequired'),
      trigger: 'blur',
    },
  ],
  username:
    dialog.type === 'add'
      ? [
          {
            required: true,
            message: t('user.errUsernameRequired'),
            trigger: 'blur',
          },
          {
            min: 3,
            max: 30,
            message: t('user.errUsernameLength'),
            trigger: 'blur',
          },
        ]
      : [],
  password:
    dialog.type === 'add'
      ? [
          {
            required: true,
            message: t('user.errPasswordRequired'),
            trigger: 'blur',
          },
          {
            min: 6,
            max: 100,
            message: t('user.errPasswordLength'),
            trigger: 'blur',
          },
        ]
      : [],
  phone:
    dialog.type === 'add'
      ? [
          {
            pattern: /^1[3-9]\d{9}$/,
            message: t('user.errPhoneFormat'),
            trigger: 'blur',
          },
        ]
      : [],
  email:
    dialog.type === 'add'
      ? [
          {
            type: 'email',
            message: t('user.errEmailFormat'),
            trigger: 'blur',
          },
        ]
      : [],
}));

const formRef = ref(null);

const originalRealname = ref('');

async function fetchData() {
  loading.value = true;
  try {
    const res = await userApi.getUserList(queryParams);
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

async function fetchGroups() {
  try {
    groupOptions.value = await groupApi.getGroupTree();
  } catch (e) {
    /* ignore */
  }
}

function handleAdd() {
  dialog.type = 'add';
  dialog.title = t('user.newUser');
  dialog.visible = true;
  resetForm();
}

async function handleEdit(row) {
  dialog.type = 'edit';
  dialog.title = t('user.editUser');
  dialog.visible = true;
  resetForm();
  let detail;
  try {
    detail = await userApi.getUserDetail(row.id);
  } catch (e) {
    detail = row;
  }
  const groups = detail.groups || [];
  const realname = detail.realname || '';
  originalRealname.value = realname;
  Object.assign(formData, {
    id: detail.id ?? row.id,
    username: '',
    password: '',
    nickname: detail.nickname || row.nickname || '',
    realname,
    gender: detail.gender || 0,
    phone: detail.phone || '',
    email: detail.email || '',
    status: detail.status,
    motto: detail.motto || '',
    birthDate: detail.birthDate || '',
    education: detail.education,
    location: detail.location || '',
    biography: detail.biography || '',
    groupIds: groups.map((g) => g.id) || [],
  });
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(t('user.confirmDelete'), t('common.hint'), {
      type: 'warning',
    });
    await userApi.deleteUser(row.id);
    ElMessage.success(t('common.success'));
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(t('user.confirmResetPassword'), t('common.hint'), {
      type: 'warning',
    });
    await userApi.resetUserPassword(row.id);
    ElMessage.success(t('user.passwordReset'));
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleStatusChange(row) {
  const newStatus = row.status === UserStatus.ENABLE ? UserStatus.DISABLE : UserStatus.ENABLE;
  try {
    await userApi.updateUserStatus(row.id, { status: newStatus });
    ElMessage.success(t('common.success'));
    fetchData();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const payload = { ...formData };
    const groupIds = [...formData.groupIds];
    delete payload.groupIds;
    if (dialog.type === 'edit') {
      delete payload.username;
      delete payload.password;
      delete payload.phone;
      delete payload.email;
      if (payload.realname === originalRealname.value) {
        delete payload.realname;
      }
      await userApi.updateUser(payload.id, payload);
      await userApi.assignGroups({ userId: payload.id, groupIds });
    } else {
      const res = await userApi.createUser(payload);
      const newId = res?.id ?? payload.id;
      if (newId != null) {
        await userApi.assignGroups({ userId: newId, groupIds });
      }
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
  originalRealname.value = '';
  Object.assign(formData, {
    id: null,
    username: '',
    password: '',
    nickname: '',
    realname: '',
    gender: 0,
    phone: '',
    email: '',
    status: 1,
    motto: '',
    birthDate: '',
    education: null,
    location: '',
    biography: '',
    groupIds: [],
  });
}

function handleQuery() {
  queryParams.page = 1;
  fetchData();
}

function resetQuery() {
  queryParams.nickname = '';
  queryParams.phone = '';
  queryParams.status = null;
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
  fetchGroups();
});
</script>

<style scoped>
.dialog-body {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 6px;
}

.self-managed-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-left: 8px;
  white-space: nowrap;
}
</style>
