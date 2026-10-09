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
  <el-tree-select
    v-model="selectedKeys"
    :data="data"
    :props="{ label: 'name', value: 'id' }"
    check-strictly
    multiple
    :placeholder="t('group.placeholderSelect')"
    clearable
    style="width: 100%" />
</template>

<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
});

const emit = defineEmits(['update:modelValue']);

const selectedKeys = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});
</script>
