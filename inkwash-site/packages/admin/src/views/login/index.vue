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
  <div class="login-container">
    <div class="login-locale-switch">
      <el-dropdown class="locale-dropdown" trigger="click" @command="handleLocaleChange">
        <div class="locale-switcher">
          <SvgIcon icon-class="language" class="locale-globe" />
          <span class="locale-text">{{ settingsStore.locale === 'zh-CN' ? '中文' : 'EN' }}</span>
          <el-icon class="locale-icon"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="zh-CN" :class="{ 'is-active': settingsStore.locale === 'zh-CN' }">
              中文
            </el-dropdown-item>
            <el-dropdown-item command="en" :class="{ 'is-active': settingsStore.locale === 'en' }">
              English
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <div class="login-card">
      <div class="login-header">
        <img src="/logo.svg" alt="logo" class="login-logo" />
        <h2 class="login-title">{{ siteStore.fullName }}</h2>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane :label="$t('login.tabPassword')" name="password">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="rules" size="large">
            <el-form-item prop="username">
              <el-input
                v-model="passwordForm.username"
                :placeholder="$t('login.placeholderUsername')"
                prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                :placeholder="$t('login.placeholderPassword')"
                prefix-icon="Lock"
                show-password
                @keyup.enter="handlePasswordLogin" />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="passwordForm.captchaCode"
                  :placeholder="$t('login.placeholderCaptcha')"
                  prefix-icon="Key"
                  class="captcha-input"
                  @keyup.enter="handlePasswordLogin" />
                <img v-if="captchaImg" :src="captchaImg" class="captcha-img" @click="fetchCaptcha" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handlePasswordLogin">{{
                $t('login.signIn')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="$t('login.tabSms')" name="sms" disabled>
          <el-alert type="warning" :closable="false" class="sms-notice">
            {{ $t('login.smsUnavailable') }}
          </el-alert>
          <el-form ref="smsFormRef" :model="smsForm" :rules="rules" size="large">
            <el-form-item prop="phone">
              <el-input v-model="smsForm.phone" :placeholder="$t('login.placeholderPhone')" prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item prop="smsCode">
              <div class="captcha-row">
                <el-input
                  v-model="smsForm.smsCode"
                  :placeholder="$t('login.placeholderCaptcha')"
                  prefix-icon="Message"
                  class="captcha-input"
                  @keyup.enter="handleSmsLogin" />
                <el-button type="primary" :disabled="smsCountdown > 0" @click="handleSendSmsCode">
                  {{ smsCountdown > 0 ? `${smsCountdown}s` : $t('login.sendCode') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleSmsLogin">{{
                $t('login.signIn')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="$t('login.tabQr')" name="qr">
          <div class="qr-container">
            <div v-if="qrImage" class="qr-wrapper">
              <div class="qr-frame">
                <img :src="qrImage" class="qr-image" alt="QR code" />
              </div>
              <p class="qr-status">{{ qrStatusText }}</p>
              <el-button link type="primary" @click="handleQrRefresh">{{ $t('login.refreshQr') }}</el-button>
            </div>
            <el-button v-else class="login-btn" @click="refreshQrCode">{{ $t('login.refreshQr') }}</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>

      <el-divider>{{ $t('login.thirdPartyLogin') }}</el-divider>
      <div class="oauth-buttons">
        <el-button circle class="oauth-btn" :title="$t('login.githubTitle')" @click="handleOAuth2Login('github')">
          <SvgIcon icon-class="github" class="oauth-icon" />
        </el-button>
        <el-button circle class="oauth-btn" :title="$t('login.alipayTitle')" @click="handleOAuth2Login('alipay')">
          <SvgIcon icon-class="alipay" class="oauth-icon" />
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue';
import {
  useAuthStore,
  useSiteStore,
  captcha as captchaApi,
  sendSmsCode as sendSmsCodeApi,
  getQrCode,
  checkQrStatus,
  createQrEventSource,
  qrLogin,
  getErrorMessage,
} from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';

import SvgIcon from '@/components/SvgIcon/index.vue';
import i18n from '@/locale/index.js';
import { useSettingsStore } from '@/stores';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const siteStore = useSiteStore();
const settingsStore = useSettingsStore();

const activeTab = ref('password');
const loading = ref(false);
const captchaImg = ref('');
const captchaKey = ref('');
const qrImage = ref('');
const qrKey = ref('');
const qrSseToken = ref('');
const qrStatusText = ref(t('login.qrStatus'));

const smsForm = reactive({
  phone: '',
  smsCode: '',
});

const smsCountdown = ref(0);
let smsTimer = null;
let qrEventSource = null;

const passwordForm = reactive({
  username: '',
  password: '',
  captchaCode: '',
});

const rules = {
  username: [
    {
      required: true,
      message: t('login.errUsernameRequired'),
      trigger: 'blur',
    },
  ],
  password: [
    {
      required: true,
      message: t('login.errPasswordRequired'),
      trigger: 'blur',
    },
  ],
  captchaCode: [
    {
      required: true,
      message: t('login.errCaptchaRequired'),
      trigger: 'blur',
    },
  ],
  phone: [
    {
      required: true,
      message: t('login.errPhoneRequired'),
      trigger: 'blur',
    },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: t('login.errPhoneFormat'),
      trigger: 'blur',
    },
  ],
  smsCode: [
    {
      required: true,
      message: t('login.errCaptchaRequired'),
      trigger: 'blur',
    },
  ],
};

const passwordFormRef = ref(null);
const smsFormRef = ref(null);

async function fetchCaptcha() {
  try {
    const data = await captchaApi();
    captchaImg.value = data.captchaImage;
    captchaKey.value = data.captchaId;
  } catch (e) {
    console.error('Failed to fetch captcha:', e);
  }
}

function closeQrSse() {
  if (qrEventSource) {
    qrEventSource.close();
    qrEventSource = null;
  }
}

async function handleQrConfirmed(data) {
  closeQrSse();
  qrStatusText.value = t('login.qrSuccess');
  try {
    await qrLogin(data.ticket);
    await authStore.bootstrap();
    ElMessage.success(t('login.loginSuccess'));
    const redirect = route.query.redirect || '/';
    router.push(validateRedirect(redirect));
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('login.loginFailed')));
    qrStatusText.value = t('login.qrFailed');
    refreshQrCode();
  }
}

function handleQrExpired() {
  qrStatusText.value = t('login.qrExpired');
  qrImage.value = '';
  refreshQrCode();
}

async function refreshQrCode() {
  closeQrSse();
  try {
    const res = await getQrCode('admin');
    qrImage.value = res.captchaImage || res.image || res.data?.captchaImage || '';
    qrKey.value = res.captchaId || res.id || res.data?.captchaId || '';
    qrSseToken.value = res.sseToken || res.data?.sseToken || '';
    qrStatusText.value = qrImage.value ? t('login.qrStatus') : t('login.qrFailed');
    if (qrImage.value && qrKey.value) {
      qrEventSource = createQrEventSource(qrKey.value, qrSseToken.value, {
        onConfirmed: handleQrConfirmed,
        onExpired: handleQrExpired,
      });
    }
  } catch (e) {
    console.error('Failed to fetch QR code:', e);
    qrStatusText.value = t('login.qrFailed');
  }
}

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
      ElMessage.success(t('login.loginSuccess'));
      const redirect = route.query.redirect || '/';
      router.push(validateRedirect(redirect));
    } else if (res.status === 'SCANNED') {
      const who = [res.nickname, res.phone].filter(Boolean).join(' ');
      qrStatusText.value = who ? t('login.qrScannedUser', { user: who }) : t('login.qrScanned');
    } else if (res.status === 'EXPIRED') {
      qrStatusText.value = t('login.qrExpired');
      qrImage.value = '';
      refreshQrCode();
    } else {
      qrStatusText.value = t('login.qrStatus');
    }
  } catch (e) {
    qrStatusText.value = t('login.qrCheckFailed');
  }
}

function handleLocaleChange(locale) {
  settingsStore.changeLocale(locale);
  i18n.global.locale.value = locale;
}

async function handleSendSmsCode() {
  const valid = await smsFormRef.value.validateField('phone').catch(() => false);
  if (!valid) return;

  try {
    await sendSmsCodeApi({ phone: smsForm.phone });
    ElMessage.success(t('login.codeSent'));
    smsCountdown.value = 60;
    smsTimer = setInterval(() => {
      smsCountdown.value--;
      if (smsCountdown.value <= 0) {
        clearInterval(smsTimer);
        smsTimer = null;
      }
    }, 1000);
  } catch (error) {
    ElMessage.error(getErrorMessage(error, t('login.codeSendFailed')));
  }
}

let oauthPopup = null;

function handleOAuth2Login(provider) {
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
}

function handleOAuth2Message(event) {
  if (event.source !== oauthPopup) {
    return;
  }
  if (event.data?.status === 'ok') {
    oauthPopup = null;
    authStore.bootstrap().then(() => {
      ElMessage.success(t('login.loginSuccess'));
      const redirect = route.query.redirect || '/';
      router.push(validateRedirect(redirect));
    });
  }
}

function validateRedirect(path) {
  if (!path || typeof path !== 'string') return '/';
  if (path === '/') return path;
  if (/^https?:\/\//.test(path) || path.startsWith('//')) return '/';
  if (!path.startsWith('/')) return '/';
  return path;
}

async function handlePasswordLogin() {
  const valid = await passwordFormRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    await authStore.login({
      authType: 1,
      identity: passwordForm.username,
      plainPassword: passwordForm.password,
      captchaId: captchaKey.value,
      inputCaptcha: passwordForm.captchaCode,
    });
    const redirect = route.query.redirect || '/';
    router.push(validateRedirect(redirect));
  } catch (error) {
    ElMessage.error(getErrorMessage(error, t('login.loginFailed')));
    fetchCaptcha();
  } finally {
    loading.value = false;
  }
}

async function handleSmsLogin() {
  const valid = await smsFormRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    await authStore.login({
      authType: 2,
      identity: smsForm.phone,
      inputSmsCode: smsForm.smsCode,
    });
    const redirect = route.query.redirect || '/';
    router.push(validateRedirect(redirect));
  } catch (error) {
    ElMessage.error(getErrorMessage(error, t('login.loginFailed')));
  } finally {
    loading.value = false;
  }
}

watch(activeTab, (tab) => {
  if (tab === 'qr') {
    refreshQrCode();
  } else {
    closeQrSse();
  }
});

onMounted(() => {
  fetchCaptcha();
  siteStore.fetchSiteInfo();
  window.addEventListener('message', handleOAuth2Message);
});

onUnmounted(() => {
  if (smsTimer) clearInterval(smsTimer);
  closeQrSse();
  window.removeEventListener('message', handleOAuth2Message);
});
</script>

<style scoped>
.sms-notice {
  margin-bottom: 16px;
}
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100vh;
  background: url('@/assets/images/login-scene.svg') center bottom / cover no-repeat;
}

.login-locale-switch {
  position: absolute;
  top: 20px;
  right: 28px;
  z-index: 2;
}

.login-locale-switch .locale-switcher {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #555;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s;
}

.login-locale-switch .locale-switcher:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(255, 255, 255, 0.7);
}

.login-locale-switch .locale-globe {
  width: 15px;
  height: 15px;
}

.login-locale-switch .locale-icon {
  font-size: 12px;
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.login-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.login-logo {
  width: 48px;
  height: 48px;
  margin-right: 12px;
}

.login-title {
  font-size: 24px;
  color: #333;
}

.login-tabs {
  margin-top: 16px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  flex-shrink: 0;
}

.login-btn {
  width: 100%;
}

.qr-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.qr-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-frame {
  padding: 12px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.qr-image {
  display: block;
  width: 140px;
  height: 140px;
}

.qr-status {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.oauth-buttons {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.oauth-btn {
  width: 44px;
  height: 44px;
}

.oauth-icon {
  font-size: 22px;
}
</style>
