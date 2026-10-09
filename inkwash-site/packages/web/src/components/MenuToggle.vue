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
  <button
    class="menu-toggle"
    :class="[margin ? `margin-${margin}` : '']"
    :aria-label="label"
    aria-expanded="false"
    @click="emit('toggle')">
    <span class="menu-icon" :class="{ open: open }"> <span></span><span></span><span></span> </span>
  </button>
</template>

<script setup>
const props = defineProps({
  open: { type: Boolean, default: false },
  label: { type: String, default: '' },
  margin: {
    type: String,
    default: '',
    validator: (v) => ['', 'left', 'right'].includes(v),
  },
});
const emit = defineEmits(['toggle']);
</script>

<style scoped>
.menu-toggle {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
}

.menu-toggle.margin-left {
  margin-left: -8px;
}

.menu-toggle.margin-right {
  margin-right: 8px;
}

.menu-icon {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 20px;
}

.menu-icon span {
  display: block;
  height: 2px;
  background: var(--text-color);
  border-radius: 2px;
  transition: all 0.3s;
}

.menu-icon.open span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}
.menu-icon.open span:nth-child(2) {
  opacity: 0;
}
.menu-icon.open span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

@media (max-width: 768px) {
  .menu-toggle {
    display: block;
  }
}
</style>
