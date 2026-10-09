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
  <div class="register-page">
    <div class="register-card">
      <div class="card-header">
        <div class="card-logo-row">
          <img src="/logo.svg" alt="logo" class="card-logo" />
          <h1 class="card-title">{{ siteStore.fullName }}</h1>
        </div>
        <p class="card-desc">{{ $t('register.desc') }}</p>
      </div>

      <el-tabs v-model="mode" class="register-tabs">
        <el-tab-pane :label="$t('register.passwordTab')" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            @keyup.enter="handlePasswordRegister">
            <el-form-item prop="username">
              <el-input v-model="passwordForm.username" :placeholder="$t('register.username')">
                <template #prefix
                  ><el-icon><User /></el-icon
                ></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="passwordForm.nickname" :placeholder="$t('register.nicknameOptional')">
                <template #prefix
                  ><el-icon><EditPen /></el-icon
                ></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                :placeholder="$t('register.password')"
                show-password>
                <template #prefix
                  ><el-icon><Lock /></el-icon
                ></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input v-model="passwordForm.captcha" :placeholder="$t('register.captcha')">
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
                  <span>{{ $t('register.reload') }}</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handlePasswordRegister" class="submit-btn">{{
                $t('register.register')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="$t('register.smsTab')" name="sms" disabled>
          <el-alert type="warning" :closable="false" class="sms-notice">
            {{ $t('register.smsUnavailable') }}
          </el-alert>
          <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules" @keyup.enter="handleSmsRegister">
            <el-form-item prop="phone">
              <el-input v-model="smsForm.phone" :placeholder="$t('register.phone')">
                <template #prefix
                  ><el-icon><Phone /></el-icon
                ></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="code">
              <div class="sms-row">
                <el-input v-model="smsForm.code" :placeholder="$t('register.smsCode')">
                  <template #prefix
                    ><el-icon><Message /></el-icon
                  ></template>
                </el-input>
                <el-button :disabled="smsCountdown > 0" @click="handleSendSms" class="sms-btn">
                  {{ smsCountdown > 0 ? `${smsCountdown}s` : $t('register.send') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input v-model="smsForm.captcha" :placeholder="$t('register.captcha')">
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
                  <span>{{ $t('register.reload') }}</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSmsRegister" class="submit-btn">{{
                $t('register.register')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { User, EditPen, Lock, Picture, Phone, Message } from '@element-plus/icons-vue';
import { register, getCaptcha, sendSmsCode, getErrorMessage, useSiteStore } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { useCountdown } from '@/composables/useCountdown';

const router = useRouter();
const siteStore = useSiteStore();
const { t } = useI18n();

const mode = ref('password');
const loading = ref(false);
const captchaImage = ref('');
const captchaId = ref('');

const passwordFormRef = ref(null);
const passwordForm = reactive({
  username: '',
  nickname: '',
  password: '',
  captcha: '',
});
const passwordRules = {
  username: [{ required: true, message: t('register.username'), trigger: 'blur' }],
  nickname: [{ max: 30, message: t('register.nicknameMax'), trigger: 'blur' }],
  password: [{ required: true, message: t('register.password'), trigger: 'blur' }],
  captcha: [{ required: true, message: t('register.captcha'), trigger: 'blur' }],
};

const smsFormRef = ref(null);
const smsForm = reactive({ phone: '', code: '', captcha: '' });
const smsRules = {
  phone: [{ required: true, message: t('register.phone'), trigger: 'blur' }],
  code: [{ required: true, message: t('register.smsCode'), trigger: 'blur' }],
  captcha: [{ required: true, message: t('register.captcha'), trigger: 'blur' }],
};
const { countdown: smsCountdown, start: startSmsCountdown } = useCountdown();

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha();
    if (res) {
      captchaImage.value = res.captchaImage || res.image || res.data?.captchaImage || '';
      captchaId.value = res.captchaId || res.id || res.data?.captchaId || '';
    }
  } catch {
    captchaImage.value = '';
  }
};

const handleSendSms = async () => {
  if (!smsForm.phone) {
    ElMessage.warning(t('register.phone'));
    return;
  }
  try {
    await sendSmsCode({
      phone: smsForm.phone,
      captchaId: captchaId.value,
      captchaCode: smsForm.captcha,
    });
    ElMessage.success(t('register.send'));
    startSmsCountdown();
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('register.failed')));
  }
};

const handlePasswordRegister = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    const res = await register({
      registerType: 'PASSWORD',
      username: passwordForm.username,
      nickname: passwordForm.nickname || undefined,
      password: passwordForm.password,
      captchaId: captchaId.value,
      captchaCode: passwordForm.captcha,
    });
    if (res.status === 4) {
      ElMessageBox.alert(t('register.pendingDesc'), t('register.pendingTitle'));
      router.push('/auth/login');
    } else {
      ElMessage.success(t('register.success'));
      router.push('/auth/login');
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('register.failed')));
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

const handleSmsRegister = async () => {
  const valid = await smsFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    const res = await register({
      registerType: 'SMS_CODE',
      phone: smsForm.phone,
      smsCode: smsForm.code,
      captchaId: captchaId.value,
      captchaCode: smsForm.captcha,
    });
    if (res.status === 4) {
      ElMessageBox.alert(t('register.pendingDesc'), t('register.pendingTitle'));
      router.push('/auth/login');
    } else {
      ElMessage.success(t('register.success'));
      router.push('/auth/login');
    }
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('register.failed')));
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  refreshCaptcha();
  siteStore.fetchSiteInfo();
});
</script>

<style scoped lang="scss">
@use '@/styles/auth-form';

.sms-notice {
  margin-bottom: 16px;
}

.register-page {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  background: url('@/assets/images/login-scene.svg') center bottom / cover no-repeat;
}

.register-card {
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
  margin-bottom: 18px;
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

.register-tabs {
  margin-top: 16px;
}

.register-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.register-tabs :deep(.el-tabs__item) {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  height: 44px;
}

.register-tabs :deep(.el-tabs__item.is-active) {
  color: var(--accent-color);
}

.register-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--accent-color);
  height: 2px;
  border-radius: 1px;
}

.register-card :deep(.el-form-item) {
  margin-bottom: 14px;
}
</style>
