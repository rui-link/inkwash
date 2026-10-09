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
  <el-dialog
    v-model="visible"
    :title="t('license.title')"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    :append-to-body="true">
    <div class="license-content">
      <!-- Trial upgrade prompt -->
      <el-alert
        v-if="edition === 'trial' && !tierExceeded && licensed"
        type="info"
        :closable="true"
        show-icon
        class="license-alert">
        {{ t('license.trialUpgrade') }}
      </el-alert>

      <!-- Tier exceeded warning -->
      <el-alert v-else-if="tierExceeded" type="warning" :closable="false" show-icon class="license-alert">
        <template #title>
          {{ t('license.tierExceeded', { edition: editionLabel }) }}
        </template>
        <template #default>
          <div class="tier-details">
            <span>{{ t('license.currentUserCount') }}：{{ currentUserCount }} / {{ maxUsersLabel }}</span>
            <span v-if="restrictedModules.length"
              >{{ t('license.restrictedModules') }}：{{ restrictedModules.join('、') }}</span
            >
          </div>
        </template>
      </el-alert>

      <!-- Invalid license error -->
      <el-alert v-else-if="!licensed" type="error" :closable="false" show-icon :title="warning" />

      <el-descriptions v-if="licenseData" :column="1" border class="license-desc">
        <el-descriptions-item :label="t('license.holder')">
          {{ licenseData.holder || '-' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="edition" :label="t('license.title')">
          {{ editionLabel }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <template #footer>
      <el-button type="primary" :loading="checking" @click="checkLicense">
        {{ t('license.recheck') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { getLicenseInfo } from '@inkwash/share';
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const visible = ref(false);
const warning = ref('');
const licenseData = ref(null);
const checking = ref(false);
const licensed = ref(true);
const tierExceeded = ref(false);
const edition = ref('');
const currentUserCount = ref(0);
const maxUsers = ref(null);
const restrictedModules = ref([]);
const CHECK_INTERVAL = 60000;
let timer = null;

const editionLabel = computed(() => {
  const key = 'license.' + edition.value;
  const translated = t(key);
  return translated !== key ? translated : edition.value;
});

const maxUsersLabel = computed(() => {
  return maxUsers.value === null || maxUsers.value === undefined ? 'Unlimited' : maxUsers.value;
});

async function checkLicense() {
  checking.value = true;
  try {
    const info = await getLicenseInfo();
    if (info) {
      licensed.value = info.licensed !== false;
      tierExceeded.value = info.tierExceeded === true;
      edition.value = info.edition || '';
      currentUserCount.value = info.currentUserCount || 0;
      maxUsers.value = info.maxUsers;
      restrictedModules.value = info.allowedModules
        ? ['system', 'cms', 'monitor'].filter((m) => !info.allowedModules.includes(m))
        : [];
      licenseData.value = info.licenseData || null;
      warning.value = info.warning || info.message || t('license.invalid');
      visible.value = !licensed.value || tierExceeded.value;
    } else {
      visible.value = false;
    }
  } catch {
    // network/auth failure: do not block the admin panel
  } finally {
    checking.value = false;
  }
}

function onLicenseError(e) {
  const data = e.detail || {};
  warning.value = data.warning || data.message || t('license.invalid');
  tierExceeded.value = data.tierExceeded === true;
  edition.value = data.edition || '';
  currentUserCount.value = data.currentUserCount || 0;
  maxUsers.value = data.maxUsers;
  restrictedModules.value = data.allowedModules
    ? ['system', 'cms', 'monitor'].filter((m) => !data.allowedModules.includes(m))
    : [];
  licenseData.value = null;
  visible.value = true;
}

onMounted(() => {
  checkLicense();
  timer = setInterval(checkLicense, CHECK_INTERVAL);
  window.addEventListener('inkwash:license-error', onLicenseError);
});

onUnmounted(() => {
  clearInterval(timer);
  window.removeEventListener('inkwash:license-error', onLicenseError);
});
</script>

<style scoped>
.license-desc {
  margin-top: 16px;
}
.tier-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  font-size: 13px;
}
</style>
