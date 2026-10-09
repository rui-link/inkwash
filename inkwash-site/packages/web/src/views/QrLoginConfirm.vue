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
  <div class="qr-confirm-page">
    <div class="qr-confirm-card">
      <div v-if="invalid" class="qr-confirm-state">
        <h2>{{ t('login.qrConfirmInvalid') }}</h2>
      </div>
      <template v-else>
        <h2 class="qr-confirm-title">{{ t('login.qrConfirmTitle') }}</h2>
        <p class="qr-confirm-desc">{{ t('login.qrConfirmDesc') }}</p>
        <p v-if="authStore.user?.username" class="qr-confirm-user">
          {{ t('login.qrConfirmUser') }}:
          <strong>{{ authStore.user.username }}</strong>
        </p>
        <div v-if="done" class="qr-confirm-success">
          {{ t('login.qrConfirmSuccess') }}
        </div>
        <div v-else-if="failed" class="qr-confirm-error">
          {{ t('login.qrConfirmFailed') }}
        </div>
        <el-button
          v-else
          type="primary"
          size="large"
          class="qr-confirm-btn"
          :loading="loading"
          :disabled="scanning"
          @click="handleConfirm"
          >{{ t('login.qrConfirmButton') }}</el-button
        >
        <el-button v-if="done || failed" class="qr-confirm-btn" size="large" @click="router.push('/')">{{
          t('login.backHome')
        }}</el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore, scanQrCode, confirmQrCode, getErrorMessage } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const authStore = useAuthStore();

const qrCodeId = computed(() => (route.query.qrCodeId || '').toString());
const invalid = computed(() => !qrCodeId.value);
const loading = ref(false);
const scanning = ref(false);
const done = ref(false);
const failed = ref(false);

const AUTO_CONFIRM_KEY = 'qr_auto_confirm';
const AUTO_CONFIRM_STATE_KEY = 'qr_auto_confirm_state';

async function handleScan() {
  if (!qrCodeId.value) return;
  scanning.value = true;
  try {
    await scanQrCode(qrCodeId.value);
  } catch {
    // 扫码上报失败由电脑端轮询的 PENDING/EXPIRED 状态体现，此处不阻断确认页
  } finally {
    scanning.value = false;
  }
}

async function handleConfirm() {
  loading.value = true;
  try {
    await confirmQrCode(qrCodeId.value);
    done.value = true;
    ElMessage.success(t('login.qrConfirmSuccess'));
  } catch (err) {
    failed.value = true;
    ElMessage.error(getErrorMessage(err, t('login.qrConfirmFailed')));
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    if (window.sessionStorage && qrCodeId.value) {
      const state = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      sessionStorage.setItem(AUTO_CONFIRM_KEY, qrCodeId.value);
      sessionStorage.setItem(AUTO_CONFIRM_STATE_KEY, state);
    }
    router.replace({
      name: 'Login',
      query: { redirect: route.fullPath },
    });
    return;
  }
  const pending = window.sessionStorage?.getItem(AUTO_CONFIRM_KEY);
  const storedState = window.sessionStorage?.getItem(AUTO_CONFIRM_STATE_KEY);
  if (pending === qrCodeId.value && storedState && pending) {
    sessionStorage.removeItem(AUTO_CONFIRM_KEY);
    sessionStorage.removeItem(AUTO_CONFIRM_STATE_KEY);
    handleConfirm();
    return;
  }
  sessionStorage.removeItem(AUTO_CONFIRM_KEY);
  sessionStorage.removeItem(AUTO_CONFIRM_STATE_KEY);
  handleScan();
});
</script>

<style scoped lang="scss">
.qr-confirm-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.qr-confirm-card {
  width: 100%;
  max-width: 420px;
  text-align: center;
}
.qr-confirm-title {
  margin: 0 0 8px;
}
.qr-confirm-desc {
  color: var(--text-secondary, #666);
  margin: 0 0 20px;
}
.qr-confirm-user {
  margin: 0 0 20px;
}
.qr-confirm-success {
  color: var(--success-color, #67c23a);
  margin-bottom: 16px;
}
.qr-confirm-error {
  color: var(--danger-color, #f56c6c);
  margin-bottom: 16px;
}
.qr-confirm-btn {
  width: 100%;
}
</style>
