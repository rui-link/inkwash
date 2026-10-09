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
    v-model="dialogVisible"
    :title="$t('about.systemInfo')"
    :width="'min(90vw, 720px)'"
    top="5vh"
    show-close
    destroy-on-close
    @close="emit('close')">
    <div v-loading="loading" class="about-body">
      <div class="about-hero">
        <div class="hero-pattern" aria-hidden="true" />
        <img src="/logo.svg" alt="logo" class="hero-logo" />
        <h3 class="hero-title">{{ aboutData.formalName || '-' }}</h3>
        <p class="hero-sub">
          {{ aboutData.shortName || '' }}
          <el-tag class="hero-version" type="primary" effect="plain" round> v{{ aboutData.version || '-' }} </el-tag>
        </p>
      </div>

      <div class="about-divider" aria-hidden="true" />

      <div class="about-desc">
        <p class="desc-text">
          {{ aboutData.description || $t('about.defaultDesc') }}
        </p>
      </div>

      <div class="about-divider" aria-hidden="true" />

      <div class="info-grid">
        <div v-for="item in infoItems" :key="item.key" class="info-item">
          <el-icon class="info-icon"><component :is="item.icon" /></el-icon>
          <div class="info-text">
            <span class="info-label">{{ item.label }}</span>
            <span v-if="!item.badge" class="info-value">{{ item.value }}</span>
            <el-tag v-else size="small" effect="plain">{{ item.value }}</el-tag>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="dialogVisible = false">{{ $t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { Box, OfficeBuilding, Timer, Cpu, Coffee, User, Stamp, Medal, Lock, Unlock } from '@element-plus/icons-vue';
import { getAboutInfo, getErrorMessage } from '@inkwash/share';
import { useLicenseStore } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, watch, computed } from 'vue';
import { useI18n } from 'vue-i18n';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
});
const emit = defineEmits(['update:modelValue', 'close']);

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const { t } = useI18n();
const licenseStore = useLicenseStore();

const loading = ref(false);
const aboutData = ref({});

const infoItems = computed(() => [
  {
    key: 'projectName',
    label: t('about.projectName'),
    value: aboutData.value.projectName || '-',
    icon: Box,
  },
  {
    key: 'formalName',
    label: t('about.formalName'),
    value: aboutData.value.formalName || '-',
    icon: OfficeBuilding,
  },
  {
    key: 'author',
    label: t('about.author'),
    value: aboutData.value.author || '-',
    icon: User,
  },
  {
    key: 'buildTime',
    label: t('about.buildTime'),
    value: aboutData.value.buildTime || '-',
    icon: Timer,
  },
  {
    key: 'edition',
    label: t('license.title'),
    value: licenseStore.edition ? t('license.' + licenseStore.edition) : '-',
    icon: licenseStore.tierExceeded ? Unlock : Lock,
  },
  {
    key: 'userCount',
    label: t('license.currentUserCount'),
    value: licenseStore.currentUserCount + ' / ' + (licenseStore.maxUsers || t('license.enterprise')),
    icon: User,
  },
  {
    key: 'copyright',
    label: t('about.copyright'),
    value: aboutData.value.copyright || '-',
    icon: Stamp,
  },
  {
    key: 'license',
    label: t('about.license'),
    value: aboutData.value.license || '-',
    icon: Medal,
    badge: true,
  },
]);

async function fetchAbout() {
  loading.value = true;
  try {
    aboutData.value = await getAboutInfo();
    licenseStore.fetchLicenseInfo();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchAbout();
    }
  },
  { immediate: false },
);
</script>

<style scoped>
.about-body {
  padding: 4px;
}

.about-hero {
  position: relative;
  text-align: center;
  padding: 28px 16px 20px;
  overflow: hidden;
}

.hero-pattern {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse 60% 55% at 50% 0%,
    color-mix(in srgb, var(--el-color-primary) 8%, transparent) 0%,
    transparent 70%
  );
  pointer-events: none;
}

.hero-logo {
  width: 56px;
  height: 56px;
  margin-bottom: 12px;
  filter: drop-shadow(0 4px 12px rgba(61, 106, 135, 0.35));
}

.hero-title {
  position: relative;
  margin: 0 0 6px;
  font-family: 'Noto Serif SC', 'STSongti', 'SimSun', serif;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--el-text-color-primary);
}

.hero-sub {
  position: relative;
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  letter-spacing: 0.2em;
}

.hero-version {
  position: relative;
}

.about-divider {
  height: 1px;
  margin: 0 20px;
  background: linear-gradient(90deg, transparent, var(--el-border-color) 50%, transparent);
}

.about-desc {
  padding: 20px 32px;
}

.desc-text {
  max-width: 560px;
  margin: 0 auto;
  text-align: center;
  font-size: 14px;
  line-height: 1.9;
  color: var(--el-text-color-regular);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 28px;
  padding: 16px 24px 24px;
}

@media (max-width: 600px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  transition: background 0.2s;
}

.info-item:hover {
  background: var(--el-fill-color-light);
}

.info-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
  border-radius: 10px;
}

.info-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.info-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.info-value {
  font-size: 14px;
  color: var(--el-text-color-primary);
  word-break: break-all;
}
</style>
