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
  <el-breadcrumb separator="/" class="app-breadcrumb">
    <el-breadcrumb-item
      v-for="(item, index) in breadcrumbs"
      :key="item.path"
      :to="index < breadcrumbs.length - 1 && item.redirect ? item.redirect : null">
      <el-icon v-if="index === 0" class="breadcrumb-home-icon"><HomeFilled /></el-icon>
      <span>{{ item.title }}</span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { HomeFilled } from '@element-plus/icons-vue';
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import i18n from '@/locale/index';
import { translateRouteTitle } from '@/utils/i18n';

const route = useRoute();
const breadcrumbs = ref([]);

function getBreadcrumbs() {
  const matched = route.matched.filter((item) => item.meta && item.meta.title);
  breadcrumbs.value = matched.map((item) => ({
    title: translateRouteTitle(item.meta.title),
    path: item.path,
    redirect: item.redirect,
  }));
}

watch(() => route.path, getBreadcrumbs, { immediate: true });
watch(() => i18n.global.locale.value, getBreadcrumbs);
</script>

<style scoped lang="scss">
.app-breadcrumb {
  line-height: $navbar-height;
}

.breadcrumb-home-icon {
  vertical-align: -2px;
  margin-right: 4px;
}
</style>
