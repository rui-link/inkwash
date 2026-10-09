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
  <div class="account-container">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="$t('account.profile')" name="profile">
          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="100px"
            style="max-width: 600px">
            <el-form-item :label="$t('avatarUpload.changeAvatar')">
              <AvatarUploader
                v-model="profileForm.avatar"
                :name="profileForm.nickname"
                @change="(file) => (avatarFile = file)" />
            </el-form-item>
            <el-form-item :label="$t('account.nickname')" prop="nickname">
              <el-input v-model="profileForm.nickname" :placeholder="$t('account.placeholderNickname')" />
            </el-form-item>
            <el-form-item :label="$t('account.realname')">
              <el-input v-model="profileForm.realname" :placeholder="$t('account.placeholderRealname')" />
            </el-form-item>
            <el-form-item :label="$t('account.gender')">
              <el-select v-model="profileForm.gender" :placeholder="$t('account.placeholderSelect')">
                <el-option :value="Gender.MALE" :label="$t('enum.gender.male')" />
                <el-option :value="Gender.FEMALE" :label="$t('enum.gender.female')" />
                <el-option :value="Gender.UNKNOWN" :label="$t('enum.gender.unknown')" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('account.phone')">
              <div class="readonly-field">
                <span class="readonly-value">{{ profileForm.phone || '—' }}</span>
                <span v-if="verifiedTypes.has('PHONE')" class="verified-tag">{{ $t('account.verified') }}</span>
              </div>
            </el-form-item>
            <el-form-item :label="$t('account.email')">
              <div class="readonly-field">
                <span class="readonly-value">{{ profileForm.email || '—' }}</span>
                <span v-if="verifiedTypes.has('EMAIL')" class="verified-tag">{{ $t('account.verified') }}</span>
              </div>
            </el-form-item>
            <el-form-item :label="$t('account.motto')">
              <el-input v-model="profileForm.motto" :placeholder="$t('account.placeholderMotto')" />
            </el-form-item>
            <el-form-item :label="$t('account.location')">
              <el-cascader
                v-model="profileForm.location"
                :options="regionOptions"
                :props="regionCascaderProps"
                clearable
                :placeholder="$t('account.placeholderLocation')"
                style="width: 100%" />
            </el-form-item>
            <el-form-item :label="$t('account.birthDate')">
              <el-date-picker
                v-model="profileForm.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t('account.placeholderBirthDate')" />
            </el-form-item>
            <el-form-item :label="$t('account.education')">
              <el-select v-model="profileForm.education" :placeholder="$t('account.placeholderEducation')" clearable>
                <el-option
                  v-for="(item, key) in UserEducationMap"
                  :key="key"
                  :label="item.label"
                  :value="Number(key)" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('account.biography')">
              <el-input
                v-model="profileForm.biography"
                type="textarea"
                :rows="3"
                :placeholder="$t('account.placeholderBiography')" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileLoading" @click="handleSaveProfile">{{
                $t('common.save')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="$t('account.security')" name="security">
          <el-form label-width="120px" style="max-width: 600px">
            <el-divider content-position="left">{{ $t('account.securityEmail') }}</el-divider>
            <el-form-item :label="$t('account.currentEmail')">
              <div class="readonly-field">
                <span class="readonly-value">{{ authStore.user?.email || $t('account.notSet') }}</span>
                <span v-if="verifiedTypes.has('EMAIL')" class="verified-tag">{{ $t('account.verified') }}</span>
              </div>
            </el-form-item>
            <el-form-item :label="$t('account.newEmail')" prop="email">
              <div class="code-row">
                <el-input v-model="securityForm.email" :placeholder="$t('account.placeholderNewEmail')" />
              </div>
            </el-form-item>
            <el-form-item :label="$t('account.emailCode')" prop="emailCode">
              <div class="code-row">
                <el-input v-model="securityForm.emailCode" :placeholder="$t('account.placeholderEmailCode')" />
                <el-button
                  :disabled="emailCountdown > 0 || !securityForm.email"
                  class="code-btn"
                  @click="handleSendEmailCode">
                  {{ emailCountdown > 0 ? $t('account.resendIn', { s: emailCountdown }) : $t('account.sendCode') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="emailLoading" @click="handleVerifyEmail">{{
                $t('account.verifyEmail')
              }}</el-button>
            </el-form-item>

            <el-divider content-position="left">{{ $t('account.phoneSettings') }}</el-divider>
            <el-form-item :label="$t('account.currentPhone')">
              <div class="readonly-field">
                <span class="readonly-value">{{ authStore.user?.phone || $t('account.notSet') }}</span>
                <span v-if="verifiedTypes.has('PHONE')" class="verified-tag">{{ $t('account.verified') }}</span>
              </div>
            </el-form-item>
            <el-form-item :label="$t('account.newPhone')" prop="phone">
              <el-input v-model="securityForm.phone" :placeholder="$t('account.placeholderNewPhone')" />
            </el-form-item>
            <el-form-item :label="$t('account.smsCode')" prop="smsCode">
              <div class="code-row">
                <el-input v-model="securityForm.smsCode" :placeholder="$t('account.placeholderSmsCode')" />
                <el-button
                  :disabled="phoneCountdown > 0 || !securityForm.phone"
                  class="code-btn"
                  @click="handleSendPhoneCode">
                  {{ phoneCountdown > 0 ? $t('account.resendIn', { s: phoneCountdown }) : $t('account.sendCode') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="phoneLoading" @click="handleVerifyPhone">{{
                $t('account.verifyPhone')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="$t('account.changePassword')" name="password">
          <el-form
            v-if="hasPassword"
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="120px"
            style="max-width: 600px">
            <el-form-item :label="$t('account.currentPassword')" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                :placeholder="$t('account.placeholderCurrentPassword')" />
            </el-form-item>
            <el-form-item :label="$t('account.newPassword')" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                :placeholder="$t('account.placeholderNewPassword')" />
            </el-form-item>
            <el-form-item :label="$t('account.confirmPassword')" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                :placeholder="$t('account.placeholderConfirmPassword')" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">{{
                $t('account.changePassword')
              }}</el-button>
            </el-form-item>
          </el-form>

          <el-form
            v-else
            ref="setPasswordFormRef"
            :model="setPasswordForm"
            :rules="setPasswordRules"
            label-width="120px"
            style="max-width: 600px">
            <el-alert
              v-if="!verifiedTypes.has('EMAIL')"
              :title="$t('account.noVerifiedEmail')"
              type="warning"
              :closable="false"
              show-icon
              class="set-password-alert" />
            <el-form-item :label="$t('account.setPassword')">
              <span class="form-hint">{{ $t('account.setPasswordDesc') }}</span>
            </el-form-item>
            <el-form-item :label="$t('account.username')" prop="username">
              <el-input v-model="setPasswordForm.username" :placeholder="$t('account.placeholderUsername')" />
            </el-form-item>
            <el-form-item :label="$t('account.emailCode')" prop="emailCode">
              <div class="code-row">
                <el-input v-model="setPasswordForm.emailCode" :placeholder="$t('account.placeholderEmailCode')" />
                <el-button
                  :disabled="setPasswordCountdown > 0 || !authStore.user?.email"
                  class="code-btn"
                  @click="handleSendSetPasswordCode">
                  {{
                    setPasswordCountdown > 0
                      ? $t('account.resendIn', { s: setPasswordCountdown })
                      : $t('account.sendCode')
                  }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item :label="$t('account.newPassword')" prop="newPassword">
              <el-input
                v-model="setPasswordForm.newPassword"
                type="password"
                show-password
                :placeholder="$t('account.placeholderNewPassword')" />
            </el-form-item>
            <el-form-item :label="$t('account.confirmPassword')" prop="confirmPassword">
              <el-input
                v-model="setPasswordForm.confirmPassword"
                type="password"
                show-password
                :placeholder="$t('account.placeholderConfirmPassword')" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="setPasswordLoading" @click="handleSetPassword">{{
                $t('account.setPassword')
              }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import {
  useAuthStore,
  updateProfile,
  changePassword,
  listAccounts,
  setPassword,
  sendPhoneVerifyCode,
  verifyPhone,
  sendEmailVerifyCode,
  verifyEmail,
  getErrorMessage,
} from '@inkwash/share';
import { AvatarUploader, uploadAvatar, UserEducationMap, Gender, regionOptions } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const authStore = useAuthStore();
const activeTab = ref('profile');
const regionCascaderProps = { checkStrictly: true, emitPath: false };

const profileFormRef = ref(null);
const passwordFormRef = ref(null);
const profileLoading = ref(false);
const passwordLoading = ref(false);

const avatarFile = ref(null);

const profileForm = reactive({
  avatar: '',
  nickname: '',
  realname: '',
  gender: 0,
  phone: '',
  email: '',
  motto: '',
  location: '',
  birthDate: '',
  education: null,
  biography: '',
});

const securityForm = reactive({
  email: '',
  emailCode: '',
  phone: '',
  smsCode: '',
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const hasPassword = ref(true);
const verifiedTypes = ref(new Set());
const emailCountdown = ref(0);
const phoneCountdown = ref(0);
const emailTimer = ref(null);
const phoneTimer = ref(null);
const setPasswordTimer = ref(null);

const emailLoading = ref(false);
const phoneLoading = ref(false);

const setPasswordFormRef = ref(null);
const setPasswordLoading = ref(false);
const setPasswordCountdown = ref(0);

const setPasswordForm = reactive({
  username: '',
  emailCode: '',
  newPassword: '',
  confirmPassword: '',
});

const profileRules = computed(() => ({
  nickname: [
    {
      required: true,
      message: t('account.ruleNicknameRequired'),
      trigger: 'blur',
    },
  ],
}));

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error(t('account.rulePasswordMismatch')));
  } else {
    callback();
  }
};

const passwordRules = computed(() => ({
  oldPassword: [
    {
      required: true,
      message: t('account.ruleCurrentPasswordRequired'),
      trigger: 'blur',
    },
  ],
  newPassword: [
    {
      required: true,
      message: t('account.ruleNewPasswordRequired'),
      trigger: 'blur',
    },
    { min: 6, message: t('account.rulePasswordMinLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('account.ruleConfirmPasswordRequired'),
      trigger: 'blur',
    },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}));

const validateSetPasswordConfirm = (rule, value, callback) => {
  if (value !== setPasswordForm.newPassword) {
    callback(new Error(t('account.rulePasswordMismatch')));
  } else {
    callback();
  }
};

const setPasswordRules = computed(() => ({
  username: [
    {
      required: true,
      message: t('account.placeholderUsername'),
      trigger: 'blur',
    },
  ],
  emailCode: [
    {
      required: true,
      message: t('account.placeholderEmailCode'),
      trigger: 'blur',
    },
  ],
  newPassword: [
    {
      required: true,
      message: t('account.ruleNewPasswordRequired'),
      trigger: 'blur',
    },
    { min: 6, message: t('account.rulePasswordMinLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('account.ruleConfirmPasswordRequired'),
      trigger: 'blur',
    },
    { validator: validateSetPasswordConfirm, trigger: 'blur' },
  ],
}));

async function loadSecurityState() {
  try {
    const res = await listAccounts();
    const accounts = Array.isArray(res) ? res : res.data || [];
    hasPassword.value = accounts.some((a) => a.type === 'PASSWORD');
    verifiedTypes.value = new Set(accounts.filter((a) => a.verified).map((a) => a.type));
  } catch {
    // 安全状态加载失败不影响页面
  }
}

onMounted(async () => {
  if (authStore.user) {
    Object.assign(profileForm, {
      avatar: authStore.user?.avatar || '',
      nickname: authStore.user.nickname || '',
      realname: authStore.user.realname || '',
      gender: authStore.user.gender || 0,
      phone: authStore.user.phone || '',
      email: authStore.user.email || '',
      motto: authStore.user.motto || '',
      location: authStore.user.location || '',
      birthDate: authStore.user.birthDate || '',
      education: authStore.user.education ?? null,
      biography: authStore.user.biography || '',
    });
  }
  await loadSecurityState();
});

onUnmounted(() => {
  clearInterval(emailTimer.value);
  clearInterval(phoneTimer.value);
  clearInterval(setPasswordTimer.value);
});

async function handleSaveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false);
  if (!valid) return;
  profileLoading.value = true;
  try {
    const payload = {
      nickname: profileForm.nickname,
      realname: profileForm.realname,
      gender: profileForm.gender,
      motto: profileForm.motto,
      location: profileForm.location,
      birthDate: profileForm.birthDate || null,
      education: profileForm.education,
      biography: profileForm.biography,
      avatar: profileForm.avatar,
    };
    if (avatarFile.value) {
      const fileView = await uploadAvatar(avatarFile.value);
      payload.avatar = fileView.url;
    }
    await updateProfile(payload);
    await authStore.getUserInfo(true);
    profileForm.avatar = payload.avatar || '';
    avatarFile.value = null;
    ElMessage.success(t('account.profileUpdated'));
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  } finally {
    profileLoading.value = false;
  }
}

async function handleSendEmailCode() {
  if (!securityForm.email) {
    ElMessage.warning(t('account.placeholderNewEmail'));
    return;
  }
  try {
    await sendEmailVerifyCode(securityForm.email);
    ElMessage.success(t('account.codeSent'));
    startCountdown(emailCountdown, emailTimer);
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  }
}

async function handleVerifyEmail() {
  if (!securityForm.email || !securityForm.emailCode) {
    ElMessage.warning(t('account.placeholderEmailCode'));
    return;
  }
  emailLoading.value = true;
  try {
    await verifyEmail({
      email: securityForm.email,
      emailCode: securityForm.emailCode,
    });
    ElMessage.success(t('account.emailVerifiedSuccess'));
    securityForm.email = '';
    securityForm.emailCode = '';
    await authStore.getUserInfo(true);
    await loadSecurityState();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  } finally {
    emailLoading.value = false;
  }
}

async function handleSendPhoneCode() {
  if (!securityForm.phone) {
    ElMessage.warning(t('account.placeholderNewPhone'));
    return;
  }
  try {
    await sendPhoneVerifyCode(securityForm.phone);
    ElMessage.success(t('account.codeSent'));
    startCountdown(phoneCountdown, phoneTimer);
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  }
}

async function handleVerifyPhone() {
  if (!securityForm.phone || !securityForm.smsCode) {
    ElMessage.warning(t('account.placeholderSmsCode'));
    return;
  }
  phoneLoading.value = true;
  try {
    await verifyPhone({
      phone: securityForm.phone,
      smsCode: securityForm.smsCode,
    });
    ElMessage.success(t('account.phoneVerifiedSuccess'));
    securityForm.phone = '';
    securityForm.smsCode = '';
    await authStore.getUserInfo(true);
    await loadSecurityState();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  } finally {
    phoneLoading.value = false;
  }
}

async function handleSendSetPasswordCode() {
  try {
    await sendEmailVerifyCode(authStore.user.email);
    ElMessage.success(t('account.codeSent'));
    startCountdown(setPasswordCountdown, setPasswordTimer);
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  }
}

async function handleSetPassword() {
  const valid = await setPasswordFormRef.value.validate().catch(() => false);
  if (!valid) return;
  setPasswordLoading.value = true;
  try {
    await setPassword({ ...setPasswordForm });
    ElMessage.success(t('account.passwordSet'));
    hasPassword.value = true;
    setPasswordForm.username = '';
    setPasswordForm.emailCode = '';
    setPasswordForm.newPassword = '';
    setPasswordForm.confirmPassword = '';
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  } finally {
    setPasswordLoading.value = false;
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false);
  if (!valid) return;
  passwordLoading.value = true;
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    });
    ElMessage.success(t('account.passwordChanged'));
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.updateFailed')));
  } finally {
    passwordLoading.value = false;
  }
}

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
</script>

<style scoped>
.account-container {
  padding: 20px;
}

.readonly-field {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 32px;
}

.readonly-value {
  font-size: 14px;
}

.verified-tag {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
  padding: 3px 10px;
  border-radius: 20px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.code-btn {
  width: 130px;
  flex-shrink: 0;
}

.form-hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.set-password-alert {
  margin-bottom: 16px;
}
</style>
