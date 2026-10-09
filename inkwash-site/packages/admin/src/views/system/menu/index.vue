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
        <span>{{ $t('menuMgmt.management') }}</span>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column prop="name" :label="$t('menuMgmt.title')" min-width="200" />
        <el-table-column prop="type" :label="$t('permission.type')" width="100">
          <template #default="{ row }">
            <el-tag :type="PermTypeMap[row.type]?.type">{{ PermTypeMap[row.type]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" :label="$t('menuMgmt.icon')" width="80">
          <template #default="{ row }">
            <el-icon v-if="menuIcon(row.icon)"><component :is="menuIcon(row.icon)" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="path" :label="$t('menuMgmt.routePath')" width="150" show-overflow-tooltip />
        <el-table-column prop="component" :label="$t('menuMgmt.component')" width="180" show-overflow-tooltip />
        <el-table-column prop="sort" :label="$t('menuMgmt.sort')" width="80" />
        <el-table-column prop="visible" :label="$t('menuMgmt.visible')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.visible ? 'success' : 'info'">{{
              row.visible ? $t('common.yes') : $t('common.no')
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="BaseStatusMap[row.status]?.type">{{ BaseStatusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { menuApi } from '@inkwash/share';
import { BaseStatusMap, PermTypeMap } from '@inkwash/share';
import { ref, onMounted } from 'vue';

import { resolveIcon } from '@/utils/icons';

function menuIcon(name) {
  return resolveIcon(name);
}

const loading = ref(false);
const treeData = ref([]);

async function fetchData() {
  loading.value = true;
  try {
    treeData.value = await menuApi.getMenuTree();
  } finally {
    loading.value = false;
  }
}

onMounted(fetchData);
</script>
