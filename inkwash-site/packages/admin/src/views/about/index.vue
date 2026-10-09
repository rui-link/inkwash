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
  <div class="about-page">
    <AboutDialog v-model="visible" @close="handleAboutClose" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import AboutDialog from '@/layout/components/AboutDialog/index.vue';

const router = useRouter();
const visible = ref(false);

onMounted(() => {
  visible.value = true;
});

function handleAboutClose() {
  visible.value = false;
  router.replace('/home');
}
</script>
