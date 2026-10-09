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
  <div v-if="error" class="error-boundary">
    <el-result icon="error" :title="t('errorPage.somethingWentWrong')">
      <template #sub-title>
        <p class="error-message">
          {{ error?.message || t('errorPage.unexpectedError') }}
        </p>
      </template>
      <template #extra>
        <el-button type="primary" @click="handleReset">{{ t('errorPage.retry') }}</el-button>
        <el-button @click="$router.push('/')">{{ t('errorPage.goHome') }}</el-button>
      </template>
    </el-result>
  </div>
  <slot v-else />
</template>
<script setup>
import { ref, onErrorCaptured } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const error = ref(null);
const errorInfo = ref('');

onErrorCaptured((err, instance, info) => {
  error.value = err;
  errorInfo.value = info;
  console.error('[ErrorBoundary]', err, info);
  return false;
});

function handleReset() {
  error.value = null;
  errorInfo.value = '';
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
}
.error-message {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  max-width: 400px;
  word-break: break-word;
}
</style>
