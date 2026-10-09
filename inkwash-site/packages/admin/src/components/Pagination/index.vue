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
  <div class="pagination-container">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  page: { type: Number, default: 1 },
  limit: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
});

const emit = defineEmits(['update:page', 'update:limit', 'pagination']);

const currentPage = ref(props.page);
const pageSize = ref(props.limit);

watch(
  () => props.page,
  (val) => {
    currentPage.value = val;
  },
);
watch(
  () => props.limit,
  (val) => {
    pageSize.value = val;
  },
);

function handleCurrentChange(val) {
  emit('update:page', val);
  emit('pagination', { page: val, limit: pageSize.value });
}

function handleSizeChange(val) {
  pageSize.value = val;
  emit('update:page', 1);
  emit('update:limit', val);
  emit('pagination', { page: 1, limit: val });
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
