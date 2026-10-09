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
  <div class="profile-page">
    <div class="profile-bg-shapes" aria-hidden="true">
      <div class="shape shape-1" />
      <div class="shape shape-2" />
    </div>

    <div class="page-header">
      <h2 class="page-title">{{ $t('profile.basicInfo') }}</h2>
      <p class="page-desc">{{ $t('profile.basicInfo') }}</p>
    </div>

    <div class="profile-card card-1">
      <div class="profile-avatar-section">
        <el-avatar :size="76" :src="form.avatar" class="profile-avatar">
          {{ form.nickname?.[0] || 'U' }}
        </el-avatar>
        <div>
          <div class="profile-name">{{ form.nickname || 'User' }}</div>
          <div class="profile-role">{{ roleLabel }}</div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
        <el-form-item :label="$t('profile.avatar')">
          <AvatarUploader v-model="form.avatar" :name="form.nickname" @change="(file) => (avatarFile = file)" />
        </el-form-item>
        <el-form-item :label="$t('profile.nickname')" prop="nickname">
          <el-input v-model="form.nickname" :placeholder="$t('profile.placeholderNickname')" />
        </el-form-item>
        <el-form-item :label="$t('profile.realname')">
          <el-input v-model="form.realname" :placeholder="$t('profile.placeholderRealname')" />
        </el-form-item>
        <el-form-item :label="$t('profile.gender')">
          <el-select v-model="form.gender" :placeholder="$t('profile.placeholderSelect')" clearable>
            <el-option v-for="(item, key) in GenderMap" :key="key" :label="item.label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('profile.email')">
          <div class="readonly-field">
            <span class="readonly-value">{{ form.email || '—' }}</span>
            <span v-if="verifiedTypes.has('EMAIL')" class="verified-tag">{{ $t('accountSecurity.verified') }}</span>
            <router-link to="/user/security" class="verify-link">{{ $t('accountSecurity.title') }}</router-link>
          </div>
        </el-form-item>
        <el-form-item :label="$t('profile.phone')">
          <div class="readonly-field">
            <span class="readonly-value">{{ form.phone || '—' }}</span>
            <span v-if="verifiedTypes.has('PHONE')" class="verified-tag">{{ $t('accountSecurity.verified') }}</span>
            <router-link to="/user/security" class="verify-link">{{ $t('accountSecurity.title') }}</router-link>
          </div>
        </el-form-item>
        <el-form-item :label="$t('profile.motto')">
          <el-input v-model="form.motto" :placeholder="$t('profile.placeholderMotto')" />
        </el-form-item>
        <el-form-item :label="$t('profile.location')">
          <el-cascader
            v-model="form.location"
            :options="regionOptions"
            :props="regionCascaderProps"
            clearable
            :placeholder="$t('profile.placeholderLocation')"
            style="width: 100%" />
        </el-form-item>
        <el-form-item :label="$t('profile.birthDate')">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="$t('profile.placeholderSelect')" />
        </el-form-item>
        <el-form-item :label="$t('profile.education')">
          <el-select v-model="form.education" :placeholder="$t('profile.placeholderSelect')" clearable>
            <el-option v-for="(item, key) in UserEducationMap" :key="key" :label="item.label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('profile.biography')">
          <el-input
            v-model="form.biography"
            type="textarea"
            :rows="3"
            :placeholder="$t('profile.placeholderBiography')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">{{ $t('common.save') }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="profile-card card-2">
      <h3 class="card-title">{{ $t('profile.changePassword') }}</h3>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="120px">
        <el-form-item :label="$t('profile.currentPassword')" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            :placeholder="$t('profile.placeholderCurrentPassword')" />
        </el-form-item>
        <el-form-item :label="$t('profile.newPassword')" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            :placeholder="$t('profile.placeholderNewPassword')" />
        </el-form-item>
        <el-form-item :label="$t('profile.confirmPassword')" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            :placeholder="$t('profile.placeholderConfirmPassword')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPassword" @click="handleChangePassword">{{
            $t('profile.submit')
          }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="profile-card card-3">
      <h3 class="card-title">{{ $t('accountSecurity.title') }}</h3>
      <p class="card-desc">{{ $t('accountSecurity.desc') }}</p>
      <router-link to="/user/security" class="binding-link">
        <AppIcon name="lock" :size="16" />
        {{ $t('accountSecurity.title') }}
        <AppIcon name="chevronRight" :size="14" />
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@inkwash/share';
import { getProfile, updateProfile, changePassword, listAccounts, getErrorMessage } from '@inkwash/share';
import { GenderMap, UserEducationMap, regionOptions } from '@inkwash/share';
import { AvatarUploader, uploadAvatar } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

import AppIcon from '@/components/AppIcon.vue';

const authStore = useAuthStore();
const { t } = useI18n();
const roleLabel = computed(() => {
  const roles = authStore.roles || [];
  if (authStore.isAdmin || roles.includes('*')) return t('profile.roleAdmin');
  if (roles.includes('editor')) return t('profile.roleEditor');
  if (roles.some((r) => r && r !== 'user')) return roles[0];
  return t('profile.roleUser');
});
const formRef = ref(null);
const passwordFormRef = ref(null);
const loading = ref(false);
const saving = ref(false);
const savingPassword = ref(false);
const avatarFile = ref(null);
const verifiedTypes = ref(new Set());
const regionCascaderProps = { checkStrictly: true, emitPath: false };

const form = reactive({
  nickname: '',
  realname: '',
  gender: null,
  email: '',
  phone: '',
  motto: '',
  location: '',
  birthDate: '',
  education: null,
  biography: '',
  avatar: '',
});
const rules = {
  nickname: [
    {
      required: true,
      message: t('profile.placeholderNickname'),
      trigger: 'blur',
    },
  ],
};

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const passwordRules = {
  oldPassword: [
    {
      required: true,
      message: t('profile.requiredCurrentPassword'),
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
      validator: (rule, value, cb) =>
        value === passwordForm.newPassword ? cb() : cb(new Error(t('profile.passwordNotMatch'))),
      trigger: 'blur',
    },
  ],
};

onMounted(() => {
  loadProfile();
  loadVerifiedTypes();
});

async function loadVerifiedTypes() {
  try {
    const res = await listAccounts();
    const accounts = Array.isArray(res) ? res : res.data || [];
    verifiedTypes.value = new Set(accounts.filter((a) => a.verified).map((a) => a.type));
  } catch {
    /* 校验状态加载失败不影响页面 */
  }
}

async function loadProfile() {
  loading.value = true;
  try {
    const profile = await getProfile();
    form.nickname = profile.nickname || '';
    form.realname = profile.realname || '';
    form.gender = profile.gender ?? null;
    form.email = profile.email || '';
    form.phone = profile.phone || '';
    form.motto = profile.motto || '';
    form.location = profile.location || '';
    form.birthDate = profile.birthDate || '';
    form.education = profile.education ?? null;
    form.biography = profile.biography || '';
    form.avatar = profile.avatar || '';
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('profile.loadFailed')));
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    let avatar = form.avatar;
    if (avatarFile.value) {
      const fileView = await uploadAvatar(avatarFile.value);
      avatar = fileView.url;
    }
    await updateProfile({
      nickname: form.nickname,
      realname: form.realname,
      gender: form.gender,
      motto: form.motto,
      location: form.location,
      birthDate: form.birthDate || null,
      education: form.education,
      biography: form.biography,
      avatar,
    });
    await authStore.getUserInfo(true);
    form.avatar = avatar;
    avatarFile.value = null;
    ElMessage.success(t('common.saveSuccess'));
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.saveFailed')));
  } finally {
    saving.value = false;
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  savingPassword.value = true;
  try {
    await changePassword({ ...passwordForm });
    ElMessage.success(t('profile.passwordChanged'));
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('profile.passwordChangeFailed')));
  } finally {
    savingPassword.value = false;
  }
}
</script>

<style scoped>
.profile-page {
  position: relative;
  max-width: 620px;
}

.profile-bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.profile-bg-shapes .shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.03;
}

.profile-bg-shapes .shape-1 {
  width: 400px;
  height: 400px;
  background: var(--accent-color);
  top: -100px;
  right: -100px;
}

.profile-bg-shapes .shape-2 {
  width: 300px;
  height: 300px;
  background: var(--accent-color);
  bottom: 50px;
  left: -80px;
}

.page-header {
  position: relative;
  z-index: 1;
  margin-bottom: 28px;
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
}

.profile-card {
  position: relative;
  z-index: 1;
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 28px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  animation: profileFadeIn 0.5s ease both;
}

.card-1 {
  animation-delay: 0s;
}
.card-2 {
  animation-delay: 0.1s;
}
.card-3 {
  animation-delay: 0.2s;
}

@keyframes profileFadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.profile-avatar {
  background: var(--accent-color) !important;
  font-family: var(--font-body);
  font-weight: 600;
  box-shadow: 0 2px 8px color-mix(in srgb, var(--accent-color) 20%, transparent);
}

.profile-name {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.profile-role {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
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

.binding-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--accent-color);
  text-decoration: none;
  padding: 10px 18px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.binding-link:hover {
  background: color-mix(in srgb, var(--accent-color) 4%, transparent);
  border-color: var(--accent-color);
  transform: translateX(2px);
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
  color: var(--text-color);
  font-family: var(--font-body);
}

.verified-tag {
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
  background: color-mix(in srgb, #10b981 8%, transparent);
  padding: 3px 10px;
  border-radius: 20px;
}

.verify-link {
  margin-left: auto;
  font-size: 13px;
  font-weight: 500;
  color: var(--accent-color);
  text-decoration: none;
}

.verify-link:hover {
  text-decoration: underline;
}

.binding-link svg:last-child {
  transition: transform 0.2s;
}

.binding-link:hover svg:last-child {
  transform: translateX(2px);
}
</style>
