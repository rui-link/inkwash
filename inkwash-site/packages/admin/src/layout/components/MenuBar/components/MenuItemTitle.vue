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
  <el-icon v-if="iconComponent" class="sub-el-icon"><component :is="iconComponent" /></el-icon>
  <SvgIcon v-else-if="isSpriteIcon" :icon-class="icon" class="sub-el-icon" />
  <SvgIcon v-else icon-class="menu" class="sub-el-icon" />
  <span v-if="title" class="title-text">{{ translateRouteTitle(title) }}</span>
</template>

<script setup>
import { computed } from 'vue';

import SvgIcon from '@/components/SvgIcon/index.vue';
import { translateRouteTitle } from '@/utils/i18n';
import { resolveIcon } from '@/utils/icons';

const props = defineProps({
  icon: { type: String, default: '' },
  title: { type: String, default: '' },
});

const iconComponent = computed(() => resolveIcon(props.icon));

const isSpriteIcon = computed(() => {
  if (!props.icon || iconComponent.value) return false;
  return /^[a-z]/.test(props.icon);
});
</script>

<style lang="scss" scoped>
.sub-el-icon {
  width: 16px !important;
  font-size: 16px !important;
  color: currentcolor;
}

.title-text {
  margin-left: 12px !important;
}

.hideMenuBar {
  .el-sub-menu,
  .el-menu-item {
    .svg-icon,
    .sub-el-icon {
      margin-left: 20px;
    }
  }
}
</style>
