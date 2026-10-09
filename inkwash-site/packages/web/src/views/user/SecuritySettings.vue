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
  <div class="security-settings">
    <h2 class="page-title">{{ $t('accountSecurity.title') }}</h2>
    <p class="page-desc">{{ $t('accountSecurity.desc') }}</p>

    <div class="security-card card-login-methods">
      <h3 class="card-title">{{ $t('accountSecurity.loginMethods') }}</h3>
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else class="method-list">
        <div v-for="item in methods" :key="item.key" class="method-item">
          <AppIcon :name="item.icon" :size="18" class="method-icon" />
          <div class="method-info">
            <div class="method-name">{{ item.label }}</div>
            <div class="method-identity">{{ item.identity || '—' }}</div>
          </div>
          <span v-if="item.showVerified" class="verified-tag">{{ $t('accountSecurity.verified') }}</span>
          <el-button v-if="item.canUnbind" text type="danger" size="small" @click="handleUnbind(item)">
            {{ $t('accountSecurity.unbind') }}
          </el-button>
          <span v-else-if="item.keepHint" class="min-hint">{{ $t('accountSecurity.unbindDisabled') }}</span>
        </div>
      </div>
    </div>

    <div v-if="!hasPassword" class="security-card card-set-password">
      <h3 class="card-title">{{ $t('accountSecurity.setPassword') }}</h3>
      <p class="card-desc">{{ $t('accountSecurity.setPasswordDesc') }}</p>
      <el-form ref="setPasswordRef" :model="setPasswordForm" :rules="setPasswordRules" label-width="120px">
        <el-form-item :label="$t('accountSecurity.username')" prop="username">
          <el-input v-model="setPasswordForm.username" :placeholder="$t('accountSecurity.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('accountSecurity.emailCode')" prop="emailCode">
          <div class="code-row">
            <el-input v-model="setPasswordForm.emailCode" :placeholder="$t('accountSecurity.emailCode')" />
            <el-button
              :disabled="emailCountdown > 0 || !authStore.user?.email"
              class="code-btn"
              @click="handleSendPasswordEmailCode">
              {{
                emailCountdown > 0
                  ? $t('accountSecurity.resendIn', { s: emailCountdown })
                  : $t('accountSecurity.sendCode')
              }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item :label="$t('profile.newPassword')" prop="newPassword">
          <el-input
            v-model="setPasswordForm.newPassword"
            type="password"
            show-password
            :placeholder="$t('profile.placeholderNewPassword')" />
        </el-form-item>
        <el-form-item :label="$t('profile.confirmPassword')" prop="confirmPassword">
          <el-input
            v-model="setPasswordForm.confirmPassword"
            type="password"
            show-password
            :placeholder="$t('profile.placeholderConfirmPassword')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPassword" @click="handleSetPassword">{{
            $t('accountSecurity.setPassword')
          }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="security-card card-verify-phone">
      <h3 class="card-title">{{ $t('accountSecurity.currentPhone') }}</h3>
      <div class="current-value">
        <span class="value-text">{{ authStore.user?.phone || '—' }}</span>
        <span v-if="authStore.user?.phone" class="verified-tag">{{ $t('accountSecurity.verified') }}</span>
      </div>
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-width="120px">
        <el-form-item :label="$t('accountSecurity.newPhone')" prop="phone">
          <div class="code-row">
            <el-input v-model="phoneForm.phone" :placeholder="$t('accountSecurity.newPhone')" />
          </div>
        </el-form-item>
        <el-form-item :label="$t('accountSecurity.smsCode')" prop="smsCode">
          <div class="code-row">
            <el-input v-model="phoneForm.smsCode" :placeholder="$t('accountSecurity.smsCode')" />
            <el-button :disabled="phoneCountdown > 0 || !phoneForm.phone" class="code-btn" @click="handleSendPhoneCode">
              {{
                phoneCountdown > 0
                  ? $t('accountSecurity.resendIn', { s: phoneCountdown })
                  : $t('accountSecurity.sendCode')
              }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPhone" @click="handleVerifyPhone">{{
            $t('profile.submit')
          }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="security-card card-verify-email">
      <h3 class="card-title">{{ $t('accountSecurity.currentEmail') }}</h3>
      <div class="current-value">
        <span class="value-text">{{ authStore.user?.email || '—' }}</span>
        <span v-if="authStore.user?.email" class="verified-tag">{{ $t('accountSecurity.verified') }}</span>
      </div>
      <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="120px">
        <el-form-item :label="$t('accountSecurity.newEmail')" prop="email">
          <el-input v-model="emailForm.email" :placeholder="$t('accountSecurity.newEmail')" />
        </el-form-item>
        <el-form-item :label="$t('accountSecurity.emailCode')" prop="emailCode">
          <div class="code-row">
            <el-input v-model="emailForm.emailCode" :placeholder="$t('accountSecurity.emailCode')" />
            <el-button
              :disabled="emailBindCountdown > 0 || !emailForm.email"
              class="code-btn"
              @click="handleSendEmailCode">
              {{
                emailBindCountdown > 0
                  ? $t('accountSecurity.resendIn', { s: emailBindCountdown })
                  : $t('accountSecurity.sendCode')
              }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingEmail" @click="handleVerifyEmail">{{
            $t('profile.submit')
          }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import {
  listAccounts,
  setPassword,
  sendPhoneVerifyCode,
  verifyPhone,
  sendEmailVerifyCode,
  verifyEmail,
  unbindAccount,
  getErrorMessage,
} from '@inkwash/share';
import { useAuthStore } from '@inkwash/share';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

import AppIcon from '@/components/AppIcon.vue';

const authStore = useAuthStore();
const { t } = useI18n();

const loading = ref(true);
const accounts = ref([]);
const hasPassword = ref(false);

const verifiedCount = computed(() => accounts.value.filter((a) => a.verified).length);

const methods = computed(() =>
  accounts.value.map((a) => {
    const isClaim = a.type === 'PHONE' || a.type === 'EMAIL';
    const showVerified = a.type !== 'PASSWORD' ? a.verified : true;
    const canUnbind = verifiedCount.value > 1;
    const keepHint = !isClaim && verifiedCount.value <= 1;
    return {
      key: `${a.type}:${a.id}:${a.identity}`,
      type: a.type,
      identity: a.provider ? `${a.provider} · ${a.identity}` : a.identity,
      label: methodLabel(a),
      icon: methodIcon(a.type),
      showVerified,
      canUnbind,
      keepHint,
      raw: a,
    };
  }),
);

function methodLabel(account) {
  const map = {
    PHONE: t('accountSecurity.phone'),
    EMAIL: t('accountSecurity.email'),
    PASSWORD: t('accountSecurity.passwordLogin'),
    QR_CODE: t('accountSecurity.qrCode'),
    OAUTH2: account.provider ? `${t('accountSecurity.oauth2')} · ${account.provider}` : t('accountSecurity.oauth2'),
  };
  return map[account.type] || account.type;
}

function methodIcon(type) {
  const map = {
    PHONE: 'send',
    EMAIL: 'comment',
    PASSWORD: 'lock',
    OAUTH2: 'share',
    QR_CODE: 'star',
  };
  return map[type] || 'lock';
}

const fetchAccounts = async () => {
  loading.value = true;
  try {
    const res = await listAccounts();
    accounts.value = Array.isArray(res) ? res : res.data || [];
    hasPassword.value = accounts.value.some((a) => a.type === 'PASSWORD');
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('accountSecurity.loadFailed')));
  } finally {
    loading.value = false;
  }
};

const handleUnbind = async (item) => {
  try {
    await ElMessageBox.confirm(t('accountSecurity.confirmUnbind', { name: item.label }), t('common.confirm'));
    await unbindAccount(item.raw.id, item.raw.type === 'PHONE' || item.raw.type === 'EMAIL' ? 'IDENTITY' : 'ACCOUNT');
    ElMessage.success(t('common.success'));
    await fetchAccounts();
    await authStore.getUserInfo(true);
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(getErrorMessage(err, t('common.operationFailed')));
    }
  }
};

// ===== 设置密码 =====
const setPasswordRef = ref(null);
const setPasswordForm = reactive({
  username: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: '',
});
const emailCountdown = ref(0);
const emailTimer = ref(null);
const savingPassword = ref(false);

const setPasswordRules = {
  username: [
    {
      required: true,
      message: t('accountSecurity.usernamePlaceholder'),
      trigger: 'blur',
    },
  ],
  emailCode: [
    {
      required: true,
      message: t('accountSecurity.emailCode'),
      trigger: 'blur',
    },
  ],
  newPassword: [
    {
      required: true,
      message: t('profile.requiredNewPassword'),
      trigger: 'blur',
    },
    { min: 6, message: t('profile.passwordMinLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('profile.requiredConfirmPassword'),
      trigger: 'blur',
    },
    {
      validator: (_, v) =>
        v === setPasswordForm.newPassword ? Promise.resolve() : Promise.reject(t('profile.passwordNotMatch')),
      trigger: 'blur',
    },
  ],
};

const handleSendPasswordEmailCode = async () => {
  try {
    await sendEmailVerifyCode(authStore.user.email);
    ElMessage.success(t('accountSecurity.codeSent'));
    startCountdown(emailCountdown, emailTimer);
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('common.operationFailed')));
  }
};

const handleSetPassword = async () => {
  const valid = await setPasswordRef.value?.validate().catch(() => false);
  if (!valid) return;
  savingPassword.value = true;
  try {
    await setPassword({ ...setPasswordForm });
    ElMessage.success(t('accountSecurity.setPasswordSuccess'));
    hasPassword.value = true;
    setPasswordForm.username = '';
    setPasswordForm.emailCode = '';
    setPasswordForm.newPassword = '';
    setPasswordForm.confirmPassword = '';
    await fetchAccounts();
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('common.operationFailed')));
  } finally {
    savingPassword.value = false;
  }
};

// ===== 绑定手机 =====
const phoneFormRef = ref(null);
const phoneForm = reactive({ phone: '', smsCode: '' });
const phoneCountdown = ref(0);
const phoneTimer = ref(null);
const savingPhone = ref(false);

const phoneRules = {
  phone: [
    { required: true, message: t('accountSecurity.newPhone'), trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: t('accountSecurity.newPhone'),
      trigger: 'blur',
    },
  ],
  smsCode: [{ required: true, message: t('accountSecurity.smsCode'), trigger: 'blur' }],
};

const handleSendPhoneCode = async () => {
  try {
    await sendPhoneVerifyCode(phoneForm.phone);
    ElMessage.success(t('accountSecurity.codeSent'));
    startCountdown(phoneCountdown, phoneTimer);
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('common.operationFailed')));
  }
};

const handleVerifyPhone = async () => {
  const valid = await phoneFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  savingPhone.value = true;
  try {
    await verifyPhone({ phone: phoneForm.phone, smsCode: phoneForm.smsCode });
    ElMessage.success(t('accountSecurity.verifySuccess'));
    phoneForm.phone = '';
    phoneForm.smsCode = '';
    await fetchAccounts();
    await authStore.getUserInfo(true);
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('common.operationFailed')));
  } finally {
    savingPhone.value = false;
  }
};

// ===== 绑定邮箱 =====
const emailFormRef = ref(null);
const emailForm = reactive({ email: '', emailCode: '' });
const emailBindCountdown = ref(0);
const emailBindTimer = ref(null);
const savingEmail = ref(false);

const emailRules = {
  email: [
    { required: true, message: t('accountSecurity.newEmail'), trigger: 'blur' },
    { type: 'email', message: t('accountSecurity.newEmail'), trigger: 'blur' },
  ],
  emailCode: [
    {
      required: true,
      message: t('accountSecurity.emailCode'),
      trigger: 'blur',
    },
  ],
};

const handleSendEmailCode = async () => {
  try {
    await sendEmailVerifyCode(emailForm.email);
    ElMessage.success(t('accountSecurity.codeSent'));
    startCountdown(emailBindCountdown, emailBindTimer);
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('common.operationFailed')));
  }
};

const handleVerifyEmail = async () => {
  const valid = await emailFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  savingEmail.value = true;
  try {
    await verifyEmail({
      email: emailForm.email,
      emailCode: emailForm.emailCode,
    });
    ElMessage.success(t('accountSecurity.verifySuccess'));
    emailForm.email = '';
    emailForm.emailCode = '';
    await fetchAccounts();
    await authStore.getUserInfo(true);
  } catch (err) {
    ElMessage.error(getErrorMessage(err, t('common.operationFailed')));
  } finally {
    savingEmail.value = false;
  }
};

function startCountdown(countdown, timer) {
  clearInterval(timer.value);
  countdown.value = 60;
  timer.value = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer.value);
      timer.value = null;
    }
  }, 1000);
}

onMounted(async () => {
  await fetchAccounts();
});

onUnmounted(() => {
  [emailTimer, phoneTimer, emailBindTimer].forEach((timer) => {
    if (timer.value) clearInterval(timer.value);
  });
});
</script>

<style scoped>
.security-settings {
  max-width: 620px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin-bottom: 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.loading-state {
  padding: 20px 0;
}

.security-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 28px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  animation: securityFadeIn 0.5s ease both;
}

.card-login-methods {
  animation-delay: 0s;
}
.card-set-password {
  animation-delay: 0.05s;
}
.card-verify-phone {
  animation-delay: 0.1s;
}
.card-verify-email {
  animation-delay: 0.15s;
}

@keyframes securityFadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin-bottom: 4px;
}

.card-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.current-value {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.value-text {
  font-size: 14px;
  font-weight: 500;
  font-family: var(--font-body);
}

.method-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.method-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.method-item:hover {
  border-color: var(--accent-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.method-icon {
  color: var(--text-secondary);
  flex-shrink: 0;
}

.method-info {
  flex: 1;
  min-width: 0;
}

.method-name {
  font-size: 14px;
  font-weight: 600;
}

.method-identity {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-family: var(--font-body);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.verified-tag {
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
  background: color-mix(in srgb, #10b981 8%, transparent);
  padding: 3px 10px;
  border-radius: 20px;
  flex-shrink: 0;
}

.min-hint {
  font-size: 11px;
  color: var(--text-secondary);
  opacity: 0.5;
  font-weight: 500;
  flex-shrink: 0;
}

.code-row {
  display: flex;
  gap: 12px;
}

.code-row .el-input {
  flex: 1;
}

.code-btn {
  width: 130px;
  flex-shrink: 0;
}
</style>
