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
  <div class="layout-options">
    <div
      class="layout-option"
      :class="{ 'is-active': modelValue === LayoutEnum.LEFT }"
      @click="updateValue(LayoutEnum.LEFT)">
      <div class="layout-item left">
        <div></div>
        <div></div>
      </div>
      <span class="layout-name">{{ $t('layout.left') }}</span>
    </div>

    <div
      class="layout-option"
      :class="{ 'is-active': modelValue === LayoutEnum.TOP }"
      @click="updateValue(LayoutEnum.TOP)">
      <div class="layout-item top">
        <div></div>
        <div></div>
      </div>
      <span class="layout-name">{{ $t('layout.top') }}</span>
    </div>
  </div>
</template>

<script setup>
import { LayoutEnum } from '@/enums';

defineProps({ modelValue: String });
const emit = defineEmits(['update:modelValue']);

function updateValue(layout) {
  emit('update:modelValue', layout);
}
</script>

<style scoped>
.layout-options {
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  gap: 24px;
  padding: 8px 0 8px 10px;
}

.layout-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &.is-active {
    border-color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }
}

.layout-item {
  position: relative;
  width: 88px;
  height: 64px;
  overflow: hidden;
  background: #f0f2f5;
  border-radius: 4px;
  box-shadow: 0 0 1px #888;

  &.top div:nth-child(1) {
    width: 100%;
    height: 30%;
    background: #1b2a47;
  }

  &.left div:nth-child(1) {
    width: 30%;
    height: 100%;
    background: #1b2a47;
  }

  &.left div:nth-child(2) {
    position: absolute;
    top: 0;
    right: 0;
    width: 70%;
    height: 30%;
    background: #fff;
    box-shadow: 0 0 1px #888;
  }
}

.layout-name {
  font-size: 12px;
  color: var(--el-text-color-regular);
}
</style>
