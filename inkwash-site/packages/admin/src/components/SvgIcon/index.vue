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
  <svg class="svg-icon" :style="{ color, width: size, height: size }" aria-hidden="true">
    <use :xlink:href="`#icon-${iconClass}`" />
  </svg>
</template>

<script setup>
defineProps({
  iconClass: { type: String, required: true },
  color: { type: String, default: '' },
  size: { type: [String, Number], default: '1em' },
});
</script>

<style scoped>
.svg-icon {
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}
</style>
