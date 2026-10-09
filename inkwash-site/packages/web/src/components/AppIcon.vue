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
  <svg v-if="iconClass" class="app-icon" :width="size" :height="size" aria-hidden="true">
    <use :xlink:href="`#icon-${iconClass}`" />
  </svg>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
});

const iconNames = {
  thumbUp: 'thumb-up',
  thumbDown: 'thumb-down',
  comment: 'comment',
  eye: 'eye',
  star: 'star',
  share: 'share',
  lock: 'lock',
  chevronRight: 'chevron-right',
  send: 'send',
  logout: 'logout',
  arrowLeft: 'arrow-left',
  brand: 'brand',
  brandFaint: 'brand-faint',
};

const iconClass = computed(() => iconNames[props.name]);
</script>

<style scoped>
.app-icon {
  display: inline-block;
  vertical-align: -0.15em;
  flex-shrink: 0;
  fill: none;
  stroke: currentColor;
  overflow: hidden;
}
</style>
