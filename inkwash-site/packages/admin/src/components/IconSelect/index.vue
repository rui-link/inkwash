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
  <div ref="iconSelectRef" class="relative">
    <el-input
      :model-value="modelValue"
      readonly
      :placeholder="$t('common.selectIcon')"
      @click.stop="visible = !visible">
      <template #prefix>
        <el-icon v-if="modelValue">
          <component :is="modelValue" />
        </el-icon>
      </template>
    </el-input>
    <div v-show="visible" class="icon-popover" @click.stop>
      <el-input v-model="search" :placeholder="$t('common.searchIcon')" clearable class="mb-2" />
      <div class="icon-grid">
        <div
          v-for="name in iconList"
          :key="name"
          class="icon-item"
          :class="{ 'icon-item--active': modelValue === name }"
          @click="selectIcon(name)">
          <el-icon :size="20">
            <component :is="name" />
          </el-icon>
          <div class="mt-1 w-full truncate text-center text-[12px]">
            {{ name }}
          </div>
        </div>
      </div>
      <div class="mt-2 flex justify-end gap-2 border-t border-[var(--el-border-color)] pt-2">
        <el-button size="small" @click="visible = false">{{ $t('common.cancel') }}</el-button>
        <el-button size="small" type="primary" @click="visible = false">{{ $t('common.confirm') }}</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import * as icons from '@element-plus/icons-vue';
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';

const props = defineProps({
  modelValue: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue']);
const visible = ref(false);
const search = ref('');
const iconSelectRef = ref(null);

const iconList = computed(() => {
  const names = Object.keys(icons);
  if (!search.value) return names;
  return names.filter((name) => name.toLowerCase().includes(search.value.toLowerCase()));
});

function selectIcon(name) {
  emit('update:modelValue', name);
  visible.value = false;
}

function handleClickOutside(e) {
  if (iconSelectRef.value && !iconSelectRef.value.contains(e.target)) {
    visible.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.icon-popover {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 2000;
  width: 400px;
  padding: 12px;
  margin-top: 4px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);
}

.icon-grid {
  max-height: 300px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px;
  cursor: pointer;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  transition: all 0.3s;
  width: calc(25% - 6px);
  box-sizing: border-box;
}

.icon-item:hover {
  border-color: var(--el-color-primary);
  transform: scale(1.1);
}

.icon-item--active {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

:deep(.el-input__prefix) {
  display: flex;
  align-items: center;
}
</style>
