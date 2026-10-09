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
        <el-form-item :label="$t('common.search')">
          <el-input
            v-model="searchQuery"
            :placeholder="$t('permission.searchPlaceholder')"
            clearable
            style="width: 320px" />
        </el-form-item>
      </el-form>
    </div>

    <el-card v-loading="loading">
      <template #header>
        <div class="flex-x-between">
          <span>{{ $t('menu.permissionManagement') }}</span>
          <el-tag type="info">{{ $t('permission.totalItems', { count: allPermissions.length }) }}</el-tag>
        </div>
      </template>

      <div v-if="pagedGroups.length === 0 && !loading" class="empty-state">
        <el-empty :description="$t('permission.emptyData')" />
      </div>

      <div v-for="group in pagedGroups" :key="group.module" class="permission-group">
        <div class="module-header">
          <el-tag type="info" size="large" effect="dark">
            {{ group.label }}
          </el-tag>
          <span class="module-name">{{ group.module }}</span>
          <el-divider direction="vertical" />
          <span class="module-count"
            >{{ group.children.reduce((s, r) => s + r.permissions.length, 0) }} {{ $t('permission.items') }}</span
          >
        </div>

        <div v-for="resGroup in group.children" :key="resGroup.resource" class="resource-group">
          <div class="resource-header">
            <el-tag size="small" type="info" effect="plain">{{ resGroup.resource }}</el-tag>
            <span class="resource-label">{{ resGroup.resource }}</span>
          </div>

          <el-table :data="resGroup.permissions" size="small" :show-header="true" class="permission-table">
            <el-table-column prop="name" :label="$t('permission.name')" min-width="140" />
            <el-table-column prop="authority" :label="$t('permission.authority')" min-width="200">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.authority }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.actions')" width="100">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="row.action === 'query' ? 'success' : row.action === 'delete' ? 'danger' : 'warning'">
                  {{ getActionLabel(row.action) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="$t('common.status')" width="80">
              <template #default="{ row }">
                <el-tag :type="BaseStatusMap[row.status]?.type" size="small">
                  {{ BaseStatusMap[row.status]?.label }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <Pagination
        :page="groupPage"
        :limit="groupPageSize"
        :total="totalGroups"
        :page-sizes="[10, 20, 50]"
        @pagination="handleGroupPagination" />
    </el-card>
  </div>
</template>
<script setup>
import { permissionApi, getErrorMessage } from '@inkwash/share';
import { BaseStatus, BaseStatusMap } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, reactive, computed, watch, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import Pagination from '@/components/Pagination/index.vue';

const { t } = useI18n();

const loading = ref(false);
const allPermissions = ref([]);
const searchQuery = ref('');
const groupPage = ref(1);
const groupPageSize = ref(20);

watch(searchQuery, () => {
  groupPage.value = 1;
});

const moduleLabels = computed(() => ({
  system: t('permission.moduleSystem'),
  cms: t('permission.moduleCms'),
  monitor: t('permission.moduleMonitor'),
}));

const ACTION_LABELS = computed(() => ({
  query: t('permission.actions.query'),
  create: t('permission.actions.create'),
  update: t('permission.actions.update'),
  delete: t('permission.actions.delete'),
  detail: t('permission.actions.detail'),
  'assign-role': t('permission.actions.assign-role'),
  'assign-group': t('permission.actions.assign-group'),
  'reset-password': t('permission.actions.reset-password'),
  'update-status': t('permission.actions.update-status'),
  review: t('permission.actions.review'),
  publish: t('permission.actions.publish'),
  retract: t('permission.actions.retract'),
  commit: t('permission.actions.commit'),
  agree: t('permission.actions.agree'),
  unagree: t('permission.actions.unagree'),
  favorite: t('permission.actions.favorite'),
  unfavorite: t('permission.actions.unfavorite'),
  share: t('permission.actions.share'),
  comment: t('permission.actions.comment'),
  reply: t('permission.actions.reply'),
  tally: t('permission.actions.tally'),
  resubmit: t('permission.actions.resubmit'),
  manage: t('permission.actions.manage'),
}));

function parseAuthority(authority) {
  if (!authority) return { module: '', resource: '', action: '' };
  const parts = authority.split(':');
  return {
    module: parts[0] || '',
    resource: parts[1] || '',
    action: parts[2] || '',
  };
}

const groupedPermissions = computed(() => {
  const list = allPermissions.value
    .map((p) => {
      const parsed = parseAuthority(p.authority);
      return { ...p, ...parsed };
    })
    .filter((p) => {
      if (!searchQuery.value) return true;
      const q = searchQuery.value.toLowerCase();
      return (
        p.name?.toLowerCase().includes(q) ||
        p.authority?.toLowerCase().includes(q) ||
        p.module?.toLowerCase().includes(q) ||
        p.resource?.toLowerCase().includes(q)
      );
    });

  const moduleMap = {};
  for (const p of list) {
    if (!moduleMap[p.module]) {
      moduleMap[p.module] = {};
    }
    if (!moduleMap[p.module][p.resource]) {
      moduleMap[p.module][p.resource] = [];
    }
    moduleMap[p.module][p.resource].push(p);
  }

  const result = [];
  for (const [mod, resources] of Object.entries(moduleMap).sort((a, b) => a[0].localeCompare(b[0]))) {
    const children = [];
    for (const [res, perms] of Object.entries(resources).sort((a, b) => a[0].localeCompare(b[0]))) {
      children.push({
        resource: res,
        permissions: perms.sort((a, b) => (a.action || '').localeCompare(b.action || '')),
      });
    }
    result.push({
      module: mod,
      label: moduleLabels.value[mod] || mod,
      children,
    });
  }
  return result;
});

const totalGroups = computed(() => groupedPermissions.value.length);

const pagedGroups = computed(() => {
  const start = (groupPage.value - 1) * groupPageSize.value;
  return groupedPermissions.value.slice(start, start + groupPageSize.value);
});

function handleGroupPagination({ page, limit }) {
  groupPage.value = page;
  groupPageSize.value = limit;
}

async function fetchData() {
  loading.value = true;
  try {
    const res = await permissionApi.getPermissionList({
      page: 1,
      size: 100,
    });
    allPermissions.value = res.list || [];
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

function getActionLabel(action) {
  return ACTION_LABELS.value[action] || action;
}

function getModuleTagType(mod) {
  const types = {
    system: '',
    cms: 'success',
    monitor: 'warning',
  };
  return types[mod] || 'info';
}

onMounted(fetchData);
</script>

<style lang="scss" scoped>
.permission-group {
  margin-bottom: 24px;
  &:last-child {
    margin-bottom: 0;
  }
}

.module-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.module-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.module-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.resource-group {
  margin-left: 16px;
  margin-bottom: 16px;
}

.resource-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.resource-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.permission-table {
  :deep(.el-table__header th) {
    background: var(--table-header-bg);
  }
}

.empty-state {
  padding: 40px 0;
}
</style>
