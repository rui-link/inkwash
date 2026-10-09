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
  <div class="app-container" v-loading="loading">
    <!-- License Info + Health Status Row -->
    <el-row :gutter="10" style="margin-bottom: 10px" class="equal-height-row">
      <el-col :span="12">
        <el-card :header="$t('license.title')" shadow="hover" class="full-height-card">
          <el-descriptions :column="4" border>
            <el-descriptions-item :label="$t('license.title')">
              <span :class="{ 'tier-exceeded': licenseStore.tierExceeded }">
                <el-icon><component :is="tierIcon" /></el-icon>
                {{ editionLabel }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item :label="$t('license.currentUserCount')">
              {{ licenseStore.currentUserCount }} / {{ maxUsersLabel }}
            </el-descriptions-item>
            <el-descriptions-item v-if="licenseStore.restrictedModules.length" :label="$t('license.restrictedModules')">
              <el-tag type="warning" effect="plain" size="small" v-for="m in licenseStore.restrictedModules" :key="m">{{
                m
              }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card :header="$t('monitor.healthStatus')" class="full-height-card">
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="$t('monitor.appStatus')">
              <el-tag :type="healthStatus.status === 'UP' ? 'success' : 'danger'">{{
                healthStatus.status || 'UNKNOWN'
              }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row -->
    <el-row :gutter="10">
      <el-col :span="8">
        <el-card>
          <div ref="heapChartRef" style="height: 300px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div ref="diskChartRef" style="height: 300px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div ref="threadChartRef" style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- System Info Row (full width) -->
    <el-row :gutter="10" style="margin-top: 10px">
      <el-col :span="24">
        <el-card :header="$t('monitor.systemInfo')">
          <el-descriptions :column="3" border>
            <el-descriptions-item :label="$t('monitor.osName')">{{
              systemInfo.system?.os || '-'
            }}</el-descriptions-item>
            <el-descriptions-item :label="$t('monitor.cpuCores')">{{
              systemInfo.system?.cpus || '-'
            }}</el-descriptions-item>
            <el-descriptions-item :label="$t('monitor.jvm')">{{
              systemInfo.java?.runtime || '-'
            }}</el-descriptions-item>
            <el-descriptions-item :label="$t('monitor.uptime')">{{
              formatUptime(metricsData.runtime?.uptime)
            }}</el-descriptions-item>
            <el-descriptions-item v-if="metricsData.heap" :label="$t('monitor.heapMemory')"
              >{{ formatBytes(metricsData.heap.used) }} /
              {{ formatBytes(metricsData.heap.committed) }}</el-descriptions-item
            >
            <el-descriptions-item v-if="metricsData.disk?.length" :label="$t('monitor.disk')"
              >{{ formatBytes(metricsData.disk[0].total - metricsData.disk[0].free) }}
              /
              {{ formatBytes(metricsData.disk[0].total) }}</el-descriptions-item
            >
            <el-descriptions-item v-if="metricsData.pool" :label="$t('monitor.pool')"
              >{{ $t('monitor.poolActive') }} {{ metricsData.pool.active }} / {{ $t('monitor.poolIdle') }}
              {{ metricsData.pool.idle }} /
              {{ $t('monitor.poolTotal') }}
              {{ metricsData.pool.total }}</el-descriptions-item
            >
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { monitorApi } from '@inkwash/share';
import * as echarts from 'echarts';
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useI18n } from 'vue-i18n';

import { useLicenseLabels } from '@/composables/useLicenseLabels';
import { resolveIcon } from '@/utils/icons';

const { t } = useI18n();
const { licenseStore, editionLabel, maxUsersLabel } = useLicenseLabels();

const tierIcon = computed(() => resolveIcon(licenseStore.tierExceeded ? 'Unlock' : 'Lock'));

const loading = ref(true);
const healthStatus = ref({});
const systemInfo = ref({});
const metricsData = ref({});

const heapChartRef = ref(null);
const diskChartRef = ref(null);
const threadChartRef = ref(null);
let heapChart = null;
let diskChart = null;
let threadChart = null;

function formatBytes(bytes) {
  if (!bytes || bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i];
}

function formatUptime(ms) {
  if (!ms) return '-';
  const s = Math.floor(ms / 1000);
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  const parts = [];
  if (d > 0) parts.push(d + t('monitor.unitDay'));
  if (h > 0) parts.push(h + t('monitor.unitHour'));
  if (m > 0) parts.push(m + t('monitor.unitMinute'));
  return parts.length ? parts.join(' ') : '<1' + t('monitor.unitMinute');
}

async function fetchData() {
  loading.value = true;
  try {
    const [health, info, metrics] = await Promise.all([
      monitorApi.getHealth(),
      monitorApi.getSystemInfo(),
      monitorApi.getMetrics(),
    ]);
    healthStatus.value = health || {};
    systemInfo.value = info || {};
    metricsData.value = metrics || {};
    licenseStore.fetchLicenseInfo();
    renderCharts();
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  const m = metricsData.value;

  if (heapChartRef.value) {
    heapChart = echarts.init(heapChartRef.value);
    heapChart.setOption({
      title: { text: t('monitor.heapMemory'), left: 'center' },
      series: [
        {
          type: 'gauge',
          detail: { formatter: '{value}%' },
          data: [{ value: m.heapUsagePercent || 0, name: t('monitor.heapMemory') }],
          axisLine: {
            lineStyle: {
              width: 15,
              color: [
                [0.3, '#67c23a'],
                [0.7, '#e6a23c'],
                [1, '#f56c6c'],
              ],
            },
          },
        },
      ],
    });
  }

  if (diskChartRef.value) {
    diskChart = echarts.init(diskChartRef.value);
    const disk = m.disk?.[0];
    const used = disk ? disk.total - disk.free : 0;
    const total = disk?.total || 1;
    diskChart.setOption({
      title: { text: t('monitor.diskUsage'), left: 'center' },
      series: [
        {
          type: 'gauge',
          detail: { formatter: '{value}%' },
          data: [
            {
              value: Math.round((used / total) * 100),
              name: t('monitor.disk'),
            },
          ],
          axisLine: {
            lineStyle: {
              width: 15,
              color: [
                [0.3, '#67c23a'],
                [0.7, '#e6a23c'],
                [1, '#f56c6c'],
              ],
            },
          },
        },
      ],
    });
  }

  if (threadChartRef.value) {
    threadChart = echarts.init(threadChartRef.value);
    const threads = m.threads || {};
    threadChart.setOption({
      title: { text: t('monitor.threads'), left: 'center' },
      series: [
        {
          type: 'gauge',
          detail: { formatter: '{value}' },
          min: 0,
          max: (threads.peak || 0) * 1.5 || 100,
          data: [{ value: threads.count || 0, name: t('monitor.threadCount') }],
          axisLine: {
            lineStyle: {
              width: 15,
              color: [
                [0.5, '#67c23a'],
                [0.8, '#e6a23c'],
                [1, '#f56c6c'],
              ],
            },
          },
        },
      ],
    });
  }
}

function handleResize() {
  heapChart?.resize();
  diskChart?.resize();
  threadChart?.resize();
}

onMounted(() => {
  fetchData();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  heapChart?.dispose();
  diskChart?.dispose();
  threadChart?.dispose();
});
</script>

<style scoped>
.tier-exceeded {
  color: #f56c6c;
  font-weight: 600;
}
.equal-height-row {
  display: flex;
}
.equal-height-row .el-col {
  display: flex;
}
.full-height-card {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.full-height-card .el-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.full-height-card .el-descriptions {
  flex: 1;
}
</style>
