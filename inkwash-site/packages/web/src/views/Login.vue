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
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <div class="card-logo-row">
          <img src="/logo.svg" alt="logo" class="card-logo" />
          <h1 class="card-title">{{ siteStore.fullName }}</h1>
        </div>
        <p class="card-desc">{{ $t('login.welcomeDesc') }}</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane :label="$t('login.passwordTab')" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            @keyup.enter="handlePasswordLogin">
            <el-form-item prop="identity">
              <el-input v-model="passwordForm.identity" :placeholder="$t('login.identity')">
                <template #prefix
                  ><el-icon><User /></el-icon
                ></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                :placeholder="$t('login.password')"
                show-password>
                <template #prefix
                  ><el-icon><Lock /></el-icon
                ></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input v-model="passwordForm.captcha" :placeholder="$t('login.captcha')">
                  <template #prefix
                    ><el-icon><Picture /></el-icon
                  ></template>
                </el-input>
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  class="captcha-image"
                  @click="refreshCaptcha"
                  alt="captcha" />
                <div v-else class="captcha-placeholder" @click="refreshCaptcha">
                  <span>{{ $t('login.reload') }}</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handlePasswordLogin" class="submit-btn">{{
                $t('login.signIn')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="$t('login.smsTab')" name="sms" disabled>
          <el-alert type="warning" :closable="false" class="sms-notice">
            {{ $t('login.smsUnavailable') }}
          </el-alert>
          <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules" @keyup.enter="handleSmsLogin">
            <el-form-item prop="phone">
              <el-input v-model="smsForm.phone" :placeholder="$t('login.phone')">
                <template #prefix
                  ><el-icon><Phone /></el-icon
                ></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="code">
              <div class="sms-row">
                <el-input v-model="smsForm.code" :placeholder="$t('login.smsCode')">
                  <template #prefix
                    ><el-icon><Message /></el-icon
                  ></template>
                </el-input>
                <el-button :disabled="smsCountdown > 0" @click="handleSendSms" class="sms-btn">
                  {{ smsCountdown > 0 ? `${smsCountdown}s` : $t('login.sendCode') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSmsLogin" class="submit-btn">{{
                $t('login.signIn')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane v-if="!isMobile" :label="$t('login.qrTab')" name="qr">
          <div class="qr-container">
            <div v-if="qrImage" class="qr-wrapper">
              <div class="qr-frame">
                <img :src="qrImage" class="qr-image" alt="qr code" />
              </div>
              <p class="qr-status">{{ qrStatusText }}</p>
              <el-button link type="primary" @click="handleQrRefresh">{{ $t('login.refreshQr') }}</el-button>
            </div>
            <el-button v-else @click="refreshQrCode" class="submit-btn">{{ $t('login.refreshQr') }}</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="oauth-divider">
        <span class="divider-line" />
        <span class="divider-text">{{ $t('login.orContinue') }}</span>
        <span class="divider-line" />
      </div>

      <div class="oauth-providers">
        <button class="oauth-icon-btn" @click="handleOAuth2Login('github')" :title="$t('login.oauthGithub')">
          <SvgIcon icon-class="github" class="oauth-icon" />
        </button>
        <button class="oauth-icon-btn" @click="handleOAuth2Login('alipay')" :title="$t('login.oauthAlipay')">
          <SvgIcon icon-class="alipay" class="oauth-icon" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { User, Lock, Picture, Phone, Message } from '@element-plus/icons-vue';
import { useAuthStore, useSiteStore, getErrorMessage } from '@inkwash/share';
import { getCaptcha, sendSmsCode, getQrCode, checkQrStatus, createQrEventSource, qrLogin } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';

import SvgIcon from '@/components/SvgIcon/index.vue';
import { useCountdown } from '@/composables/useCountdown';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const siteStore = useSiteStore();
const { t } = useI18n();

function safeRedirect(value) {
  if (!value || typeof value !== 'string') return '/';
  try {
    const url = new URL(value, window.location.origin);
    if (url.origin !== window.location.origin) return '/';
    const path = url.pathname + url.search + url.hash;
    if (!path.startsWith('/')) return '/';
    return path;
  } catch {
    if (value.startsWith('/') && !value.startsWith('//')) return value;
    return '/';
  }
}

const activeTab = ref('password');
const isMobile = computed(() => /Android|iPhone|iPad|Mobile|Windows Phone/i.test(navigator.userAgent || ''));
const loading = ref(false);

const passwordFormRef = ref(null);
const passwordForm = reactive({
  identity: '',
  password: '',
  captcha: '',
  captchaId: '',
});
const passwordRules = {
  identity: [{ required: true, message: t('login.identity'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.password'), trigger: 'blur' }],
  captcha: [{ required: true, message: t('login.captcha'), trigger: 'blur' }],
};
const captchaImage = ref('');

const smsFormRef = ref(null);
const smsForm = reactive({ phone: '', code: '' });
const smsRules = {
  phone: [{ required: true, message: t('login.phone'), trigger: 'blur' }],
  code: [{ required: true, message: t('login.smsCode'), trigger: 'blur' }],
};
const { countdown: smsCountdown, start: startSmsCountdown } = useCountdown();

const qrImage = ref('');
const qrKey = ref('');
const qrSseToken = ref('');
const qrStatusText = ref(t('login.qrStatus'));
let qrEventSource = null;
let qrRefreshTimer = null;
let autoRefreshCount = 0;
const MAX_AUTO_REFRESH = 3;

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha();
    if (res) {
      captchaImage.value = res.captchaImage || res.image || res.data?.captchaImage || '';
      passwordForm.captchaId = res.captchaId || res.id || res.data?.captchaId || '';
    }
  } catch {
    captchaImage.value = '';
  }
};

const handlePasswordLogin = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await authStore.login({
      authType: 1,
      identity: passwordForm.identity,
      plainPassword: passwordForm.password,
      inputCaptcha: passwordForm.captcha,
      captchaId: passwordForm.captchaId,
    });
    ElMessage.success(t('login.signInSuccess'));
    router.push(safeRedirect(route.query.redirect));
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('login.loginFailed')));
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

const handleSendSms = async () => {
  if (!smsForm.phone) {
    ElMessage.warning(t('login.phone'));
    return;
  }
  try {
    await sendSmsCode({ phone: smsForm.phone });
    ElMessage.success(t('login.sendCode'));
    startSmsCountdown();
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('login.loginFailed')));
  }
};

const handleSmsLogin = async () => {
  const valid = await smsFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await authStore.login({
      authType: 2,
      identity: smsForm.phone,
      inputSmsCode: smsForm.code,
    });
    ElMessage.success(t('login.signInSuccess'));
    router.push(safeRedirect(route.query.redirect));
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('login.loginFailed')));
  } finally {
    loading.value = false;
  }
};

const closeQrSse = () => {
  if (qrRefreshTimer) {
    clearTimeout(qrRefreshTimer);
    qrRefreshTimer = null;
  }
  if (qrEventSource) {
    qrEventSource.close();
    qrEventSource = null;
  }
};

const handleQrConfirmed = async (data) => {
  closeQrSse();
  qrStatusText.value = t('login.qrSuccess');
  try {
    await qrLogin(data.ticket);
    await authStore.bootstrap();
    ElMessage.success(t('login.signInSuccess'));
    router.push(safeRedirect(route.query.redirect));
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('login.loginFailed')));
    qrStatusText.value = t('login.qrFailed');
    refreshQrCode();
  }
};

const handleQrExpired = () => {
  qrStatusText.value = t('login.qrExpired');
  qrImage.value = '';
  if (autoRefreshCount >= MAX_AUTO_REFRESH) {
    qrStatusText.value = t('login.qrFailed');
    return;
  }
  autoRefreshCount++;
  const delay = Math.min(1000 * Math.pow(2, autoRefreshCount - 1), 5000);
  qrRefreshTimer = setTimeout(() => refreshQrCode(), delay);
};

const refreshQrCode = async () => {
  closeQrSse();
  try {
    const res = await getQrCode('web', window.location.origin);
    qrImage.value = res.captchaImage || res.image || res.data?.captchaImage || '';
    qrKey.value = res.captchaId || res.id || res.data?.captchaId || '';
    qrSseToken.value = res.sseToken || res.data?.sseToken || '';
    qrStatusText.value = qrImage.value ? t('login.qrStatus') : t('login.qrFailed');
    if (qrImage.value && qrKey.value) {
      autoRefreshCount = 0;
      qrEventSource = createQrEventSource(qrKey.value, qrSseToken.value, {
        onConfirmed: handleQrConfirmed,
        onExpired: handleQrExpired,
      });
    }
  } catch {
    qrStatusText.value = t('login.qrFailed');
  }
};

async function handleQrRefresh() {
  if (!qrImage.value || !qrKey.value) {
    refreshQrCode();
    return;
  }
  try {
    const res = await checkQrStatus({
      qrCodeId: qrKey.value,
      sseToken: qrSseToken.value,
    });
    if (res.status === 'CONFIRMED') {
      qrStatusText.value = t('login.qrSuccess');
      await qrLogin(res.ticket);
      await authStore.bootstrap();
      ElMessage.success(t('login.signInSuccess'));
      router.push(safeRedirect(route.query.redirect));
    } else if (res.status === 'SCANNED') {
      const who = [res.nickname, res.phone].filter(Boolean).join(' ');
      qrStatusText.value = who ? t('login.qrScannedUser', { user: who }) : t('login.qrScanned');
    } else if (res.status === 'EXPIRED') {
      qrStatusText.value = t('login.qrExpired');
      qrImage.value = '';
      if (autoRefreshCount < MAX_AUTO_REFRESH) {
        autoRefreshCount++;
        refreshQrCode();
      }
    } else {
      qrStatusText.value = t('login.qrStatus');
    }
  } catch {
    qrStatusText.value = t('login.qrCheckFailed');
  }
}

watch(activeTab, (tab) => {
  if (tab === 'qr') {
    refreshQrCode();
  } else {
    closeQrSse();
  }
});
let oauthPopup = null;

const handleOAuth2Login = (provider) => {
  if (provider !== 'github') {
    ElMessage.info(t('login.notImplemented'));
    return;
  }
  const width = 600,
    height = 700;
  const left = window.screen.width / 2 - width / 2;
  const top = window.screen.height / 2 - height / 2;
  oauthPopup = window.open(
    `/oauth2/authorization/${provider}`,
    'oauth2',
    `width=${width},height=${height},left=${left},top=${top},menubar=0,toolbar=0,location=0,status=0`,
  );
};

const handleOAuth2Message = async (event) => {
  if (event.source !== oauthPopup) return;
  if (event.data?.status === 'ok') {
    oauthPopup = null;
    await authStore.bootstrap();
    ElMessage.success(t('login.signInSuccess'));
    const redirect = safeRedirect(route.query.redirect);
    router.push(redirect);
  }
};

onMounted(() => {
  refreshCaptcha();
  siteStore.fetchSiteInfo();
  window.addEventListener('message', handleOAuth2Message);
});
onUnmounted(() => {
  closeQrSse();
  window.removeEventListener('message', handleOAuth2Message);
});
</script>

<style scoped lang="scss">
@use '@/styles/auth-form';

.sms-notice {
  margin-bottom: 16px;
}

.login-page {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  background: url('@/assets/images/login-scene.svg') center bottom / cover no-repeat;
}

@keyframes mistDrift {
  from {
    transform: translateX(-3%);
  }
  to {
    transform: translateX(3%);
  }
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: var(--surface-bg);
  border-radius: 12px;
  padding: 40px;
  box-shadow:
    0 24px 60px -20px rgba(31, 58, 74, 0.35),
    0 4px 16px rgba(31, 58, 74, 0.12);
  animation: cardEntrance 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes cardEntrance {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 20px;
}

.card-logo-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 6px;
}

.card-logo {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.card-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.1;
  white-space: nowrap;
}

.card-desc {
  font-size: 14px;
  color: var(--text-secondary);
  letter-spacing: 0.01em;
}

.login-tabs {
  margin-top: 16px;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.login-tabs :deep(.el-tabs__item) {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  height: 44px;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: var(--accent-color);
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--accent-color);
  height: 2px;
  border-radius: 1px;
}

.qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.qr-frame {
  width: 180px;
  height: 180px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-bg);
}

.qr-image {
  width: 100%;
  height: 100%;
}

.qr-status {
  margin-top: 16px;
  font-size: 14px;
  color: var(--text-secondary);
}

.oauth-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0 12px;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.divider-text {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  font-family: var(--font-body);
  letter-spacing: 0.03em;
}

.oauth-providers {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.oauth-icon-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-bg);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.25s;
}

.oauth-icon-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 4%, transparent);
  transform: translateY(-1px);
}

.oauth-icon {
  font-size: 20px;
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px 20px;
  }
  .card-title {
    font-size: 22px;
  }
  .captcha-row,
  .sms-row {
    flex-direction: column;
    gap: 8px;
  }
  .sms-btn {
    width: 100%;
  }
}
</style>
