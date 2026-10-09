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
  <div class="home-container" v-loading="loading">
    <el-row :gutter="16" style="margin-top: 10px">
      <el-col :xs="24" :lg="24">
        <el-card :header="$t('license.title')" shadow="hover">
          <el-descriptions :column="4" border class="license-desc">
            <el-descriptions-item :label="$t('license.holder')">
              {{ licenseStore.holder || '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="$t('license.title')">
              <span :class="{ 'tier-exceeded': licenseStore.tierExceeded }">
                <el-icon><component :is="licenseStore.tierExceeded ? Unlock : Lock" /></el-icon>
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
    </el-row>

    <template v-if="showCharts">
      <el-row :gutter="16" style="margin-top: 10px">
        <el-col v-for="card in statCards" :key="card.label" :xs="24" :sm="12" :lg="6">
          <el-card shadow="hover">
            <el-statistic :title="card.label" :value="card.value">
              <template #prefix>
                <el-icon :style="{ color: card.color }"><component :is="card.icon" /></el-icon>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <template v-if="isAdminView">
        <el-row :gutter="16" style="margin-top: 10px">
          <el-col :xs="24" :lg="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>{{ $t('home.articleStat') }}</span>
                  <div class="range-switch">
                    <el-radio-group v-model="articleRange" size="small">
                      <el-radio-button value="last7d">{{ $t('home.rangeLast7d') }}</el-radio-button>
                      <el-radio-button value="last30d">{{ $t('home.rangeLast30d') }}</el-radio-button>
                      <el-radio-button value="last12m">{{ $t('home.rangeLast12m') }}</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>
              </template>
              <el-empty v-if="adminArticleEmpty" :description="$t('home.noData')" />
              <v-chart v-else :option="adminArticleOption" style="height: 320px" autoresize />
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>{{ $t('home.userStat') }}</span>
                  <div class="range-switch">
                    <el-radio-group v-model="userRange" size="small">
                      <el-radio-button value="last7d">{{ $t('home.rangeLast7d') }}</el-radio-button>
                      <el-radio-button value="last30d">{{ $t('home.rangeLast30d') }}</el-radio-button>
                      <el-radio-button value="last12m">{{ $t('home.rangeLast12m') }}</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>
              </template>
              <el-empty v-if="userEmpty" :description="$t('home.noData')" />
              <v-chart v-else :option="userOption" style="height: 320px" autoresize />
            </el-card>
          </el-col>
        </el-row>
      </template>

      <template v-else-if="isEditorView">
        <el-row :gutter="16" style="margin-top: 10px">
          <el-col :xs="24" :lg="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>{{ $t('home.articleStat') }}</span>
                  <div class="range-switch">
                    <el-radio-group v-model="articleRange" size="small">
                      <el-radio-button value="last7d">{{ $t('home.rangeLast7d') }}</el-radio-button>
                      <el-radio-button value="last30d">{{ $t('home.rangeLast30d') }}</el-radio-button>
                      <el-radio-button value="last12m">{{ $t('home.rangeLast12m') }}</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>
              </template>
              <el-empty v-if="editorArticleEmpty" :description="$t('home.noData')" />
              <v-chart v-else :option="editorArticleOption" style="height: 320px" autoresize />
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>{{ $t('home.categoryStat') }}</span>
                  <div class="range-switch">
                    <el-radio-group v-model="categoryRange" size="small">
                      <el-radio-button value="last7d">{{ $t('home.rangeLast7d') }}</el-radio-button>
                      <el-radio-button value="last30d">{{ $t('home.rangeLast30d') }}</el-radio-button>
                      <el-radio-button value="last12m">{{ $t('home.rangeLast12m') }}</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>
              </template>
              <el-empty v-if="categoryEmpty" :description="$t('home.noData')" />
              <v-chart v-else :option="categoryOption" style="height: 320px" autoresize />
            </el-card>
          </el-col>
        </el-row>
      </template>

      <template v-else-if="isUserView">
        <el-row :gutter="16" style="margin-top: 10px">
          <el-col :xs="24" :lg="12">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>{{ $t('home.myArticleStat') }}</span>
                  <div class="range-switch">
                    <el-radio-group v-model="myArticleRange" size="small">
                      <el-radio-button value="thisMonth">{{ $t('home.rangeThisMonth') }}</el-radio-button>
                      <el-radio-button value="lastMonth">{{ $t('home.rangeLastMonth') }}</el-radio-button>
                      <el-radio-button value="thisYear">{{ $t('home.rangeThisYear') }}</el-radio-button>
                      <el-radio-button value="lastYear">{{ $t('home.rangeLastYear') }}</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>
                <div v-if="showMonthToDate || vsShown" class="card-subtitle">
                  <span v-if="showMonthToDate" class="mtd">{{ $t('home.monthToDate') }}</span>
                  <span v-if="vsShown" class="vs" :class="{ negative: vsNegative }"
                    >{{ $t('home.vsLastMonth') }} {{ vsText }}</span
                  >
                </div>
              </template>
              <el-empty v-if="myArticleEmpty" :description="$t('home.noData')" />
              <v-chart v-else :option="myArticleOption" style="height: 320px" autoresize />
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="12">
            <el-card class="chart-card">
              <template #header>
                <div class="card-header">
                  <span>{{ $t('home.interactionStat') }}</span>
                </div>
              </template>
              <el-empty v-if="interactionEmpty" :description="$t('home.noData')" />
              <v-chart v-else :option="interactionOption" style="height: 320px" autoresize />
            </el-card>
          </el-col>
        </el-row>
      </template>
    </template>

    <el-row v-else style="margin-top: 10px">
      <el-col :xs="24">
        <el-card shadow="hover">
          <template #header>{{ $t('home.welcome') }}</template>
          <span>{{ $t('home.welcome') }}</span>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { CircleCheck, Clock, Document, Lock, Unlock, User, Warning } from '@element-plus/icons-vue';
import {
  getArticleStats,
  getCategoryStats,
  getCmsDashboard,
  getDashboardSummary,
  getMyArticleStats,
  getUserStats,
  useAuthStore,
} from '@inkwash/share';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { computed, onMounted, ref, watch } from 'vue';
import VChart from 'vue-echarts';
import { useI18n } from 'vue-i18n';

import { useLicenseLabels } from '@/composables/useLicenseLabels';

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent]);

const { t } = useI18n();
const authStore = useAuthStore();
const { licenseStore, editionLabel, maxUsersLabel } = useLicenseLabels();

const loading = ref(true);
const summary = ref({});
const articleStats = ref([]);
const userStats = ref([]);
const categoryStats = ref([]);
const myArticleStats = ref([]);
const prevCreated = ref(null);
const myInteraction = ref({});
const myArticleRange = ref('thisMonth');

const articleRange = ref('last7d');
const userRange = ref('last7d');
const categoryRange = ref('last7d');

// Must stay in sync with SecurityUtil.ADMIN_AUTHORITIES and
// DashboardStatsServiceImpl.ADMIN_ROLES. ROLE_SUPER is deliberately absent: it is not
// seeded in init-data.sql, so no account can ever hold it. — ISS-021 / D-03
const isAdminView = computed(() => authStore.roles.some((r) => ['ROLE_SYSTEM', 'ROLE_ADMIN'].includes(r)));
const isEditorView = computed(() => authStore.roles.includes('ROLE_EDITOR'));
const isUserView = computed(() => !isAdminView.value && !isEditorView.value);
const showCharts = computed(() => isAdminView.value || isEditorView.value || isUserView.value);

const PALETTE = [
  '#409eff',
  '#67c23a',
  '#e6a23c',
  '#f56c6c',
  '#909399',
  '#9c27b0',
  '#00bcd4',
  '#ff9800',
  '#795548',
  '#3f51b5',
  '#8bc34a',
  '#ff5722',
];

function formatLabel(range, timeAxis) {
  return range === 'last12m' ? timeAxis : timeAxis.slice(5);
}

function formatMyLabel(timeAxis) {
  return myArticleRange.value === 'thisYear' || myArticleRange.value === 'lastYear' ? timeAxis : timeAxis.slice(5);
}

const statCards = computed(() => {
  if (isAdminView.value) {
    return [
      {
        label: t('home.totalArticles'),
        value: summary.value.totalArticles || 0,
        icon: Document,
        color: '#409eff',
      },
      {
        label: t('home.pendingReview'),
        value: summary.value.pendingReview || 0,
        icon: Clock,
        color: '#e6a23c',
      },
      {
        label: t('home.published'),
        value: summary.value.published || 0,
        icon: CircleCheck,
        color: '#67c23a',
      },
      {
        label: t('home.totalUsers'),
        value: summary.value.totalUsers || 0,
        icon: User,
        color: '#909399',
      },
    ];
  }
  if (isEditorView.value) {
    return [
      {
        label: t('home.totalArticles'),
        value: summary.value.totalArticles || 0,
        icon: Document,
        color: '#409eff',
      },
      {
        label: t('home.pendingReview'),
        value: summary.value.pendingReview || 0,
        icon: Clock,
        color: '#e6a23c',
      },
      {
        label: t('home.approved'),
        value: summary.value.approved || 0,
        icon: CircleCheck,
        color: '#67c23a',
      },
      {
        label: t('home.rejected'),
        value: summary.value.rejected || 0,
        icon: Warning,
        color: '#f56c6c',
      },
    ];
  }
  return [];
});

const adminArticleOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {
    data: [t('home.created'), t('home.approved'), t('home.rejected'), t('home.published')],
    top: 0,
    left: 'center',
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    outerBoundsMode: 'same',
    outerBoundsContain: 'axisLabel',
  },
  xAxis: {
    type: 'category',
    data: articleStats.value.map((b) => formatLabel(articleRange.value, b.timeAxis)),
  },
  yAxis: { type: 'value' },
  color: ['#409eff', '#e6a23c', '#f56c6c', '#67c23a'],
  series: [
    {
      name: t('home.created'),
      type: 'bar',
      data: articleStats.value.map((b) => b.created),
    },
    {
      name: t('home.approved'),
      type: 'bar',
      data: articleStats.value.map((b) => b.approved),
    },
    {
      name: t('home.rejected'),
      type: 'bar',
      data: articleStats.value.map((b) => b.rejected),
    },
    {
      name: t('home.published'),
      type: 'bar',
      data: articleStats.value.map((b) => b.published),
    },
  ],
}));

const editorArticleOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {
    data: [t('home.created'), t('home.pending'), t('home.pendingPublish')],
    top: 0,
    left: 'center',
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    outerBoundsMode: 'same',
    outerBoundsContain: 'axisLabel',
  },
  xAxis: {
    type: 'category',
    data: articleStats.value.map((b) => formatLabel(articleRange.value, b.timeAxis)),
  },
  yAxis: { type: 'value' },
  color: ['#409eff', '#e6a23c', '#67c23a'],
  series: [
    {
      name: t('home.created'),
      type: 'bar',
      data: articleStats.value.map((b) => b.created),
    },
    {
      name: t('home.pending'),
      type: 'bar',
      data: articleStats.value.map((b) => b.pending),
    },
    {
      name: t('home.pendingPublish'),
      type: 'bar',
      data: articleStats.value.map((b) => b.pendingPublish),
    },
  ],
}));

const userOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {
    data: [t('home.newUsers'), t('home.activeAuthors')],
    top: 0,
    left: 'center',
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    outerBoundsMode: 'same',
    outerBoundsContain: 'axisLabel',
  },
  xAxis: {
    type: 'category',
    data: userStats.value.map((b) => formatLabel(userRange.value, b.timeAxis)),
  },
  yAxis: { type: 'value' },
  color: ['#409eff', '#67c23a'],
  series: [
    {
      name: t('home.newUsers'),
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.15 },
      data: userStats.value.map((b) => b.newUsers),
    },
    {
      name: t('home.activeAuthors'),
      type: 'line',
      smooth: true,
      data: userStats.value.map((b) => b.activeAuthors),
    },
  ],
}));

const categoryMeta = computed(() => {
  const seen = [];
  const map = new Map();
  for (const bucket of categoryStats.value) {
    for (const c of bucket.categories || []) {
      const key = c.categoryId ?? 'uncategorized';
      if (!map.has(key)) {
        map.set(key, c.categoryName || t('home.uncategorized'));
        seen.push({ key, label: c.categoryName || t('home.uncategorized') });
      }
    }
  }
  return seen;
});

const categoryOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {
    data: categoryMeta.value.map((m) => m.label),
    type: 'scroll',
    top: 0,
    left: 'center',
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    outerBoundsMode: 'same',
    outerBoundsContain: 'axisLabel',
  },
  xAxis: {
    type: 'category',
    data: categoryStats.value.map((b) => formatLabel(categoryRange.value, b.timeAxis)),
  },
  yAxis: { type: 'value' },
  color: PALETTE,
  series: categoryMeta.value.map((m, i) => ({
    name: m.label,
    type: 'bar',
    stack: 'total',
    data: categoryStats.value.map((b) => {
      const found = (b.categories || []).find((c) => (c.categoryId ?? 'uncategorized') === m.key);
      return found ? found.count : 0;
    }),
    itemStyle: { color: PALETTE[i % PALETTE.length] },
  })),
}));

const myArticleOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {
    data: [t('home.pending'), t('home.approved'), t('home.rejected'), t('home.published')],
    top: 0,
    left: 'center',
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    outerBoundsMode: 'same',
    outerBoundsContain: 'axisLabel',
  },
  xAxis: {
    type: 'category',
    data: myArticleStats.value.map((b) => formatMyLabel(b.timeAxis)),
  },
  yAxis: { type: 'value' },
  color: ['#e6a23c', '#67c23a', '#f56c6c', '#409eff'],
  series: [
    {
      name: t('home.pending'),
      type: 'bar',
      data: myArticleStats.value.map((b) => b.pending),
    },
    {
      name: t('home.approved'),
      type: 'bar',
      data: myArticleStats.value.map((b) => b.approved),
    },
    {
      name: t('home.rejected'),
      type: 'bar',
      data: myArticleStats.value.map((b) => b.rejected),
    },
    {
      name: t('home.published'),
      type: 'bar',
      data: myArticleStats.value.map((b) => b.published),
    },
  ],
}));

const interactionOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', left: 'left' },
  color: ['#409eff', '#e6a23c', '#67c23a'],
  series: [
    {
      name: t('home.interactionStat'),
      type: 'pie',
      radius: ['40%', '60%'],
      center: ['55%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      data: [
        {
          name: t('home.comments'),
          value: myInteraction.value.myComments || 0,
        },
        {
          name: t('home.favorites'),
          value: myInteraction.value.myFavorites || 0,
        },
        { name: t('home.agrees'), value: myInteraction.value.myAgrees || 0 },
      ],
    },
  ],
}));

const adminArticleEmpty = computed(() => articleStats.value.length === 0);
const editorArticleEmpty = computed(() => articleStats.value.length === 0);
const userEmpty = computed(() => userStats.value.length === 0);
const categoryEmpty = computed(() => categoryStats.value.length === 0);

const myArticleEmpty = computed(() => myArticleStats.value.length === 0);
const interactionEmpty = computed(() => Object.keys(myInteraction.value).length === 0);

const myCreatedTotal = computed(() => myArticleStats.value.reduce((s, it) => s + (it.created || 0), 0));
const vsShown = computed(() => prevCreated.value !== null && prevCreated.value !== undefined);
const vsNegative = computed(() => {
  const diff = myCreatedTotal.value - Number(prevCreated.value ?? 0);
  return diff < 0;
});
const vsText = computed(() => {
  const diff = myCreatedTotal.value - Number(prevCreated.value ?? 0);
  return (diff > 0 ? '+' : '') + diff;
});
const showMonthToDate = computed(() => ['thisMonth', 'thisYear'].includes(myArticleRange.value));

async function fetchArticleStats() {
  try {
    articleStats.value = (await getArticleStats(articleRange.value)) || [];
  } catch (e) {
    console.error(e);
    articleStats.value = [];
  }
}

async function fetchUserStats() {
  try {
    userStats.value = (await getUserStats(userRange.value)) || [];
  } catch (e) {
    console.error(e);
    userStats.value = [];
  }
}

async function fetchCategoryStats() {
  try {
    categoryStats.value = (await getCategoryStats(categoryRange.value)) || [];
  } catch (e) {
    console.error(e);
    categoryStats.value = [];
  }
}

async function fetchSummary() {
  try {
    summary.value = (await getDashboardSummary()) || {};
  } catch (e) {
    console.error(e);
    summary.value = {};
  }
}

async function fetchMyArticleStats() {
  try {
    const d = await getMyArticleStats(myArticleRange.value);
    myArticleStats.value = d.items || [];
    prevCreated.value = d.prevCreated;
  } catch (e) {
    console.error(e);
    myArticleStats.value = [];
    prevCreated.value = null;
  }
}

async function fetchMyInteraction() {
  try {
    myInteraction.value = (await getCmsDashboard()) || {};
  } catch (e) {
    console.error(e);
    myInteraction.value = {};
  }
}

async function load() {
  loading.value = true;
  try {
    if (isUserView.value) {
      await Promise.all([fetchMyArticleStats(), fetchMyInteraction()]);
      return;
    }
    await Promise.all([
      fetchArticleStats(),
      fetchSummary(),
      isAdminView.value ? fetchUserStats() : fetchCategoryStats(),
    ]);
  } finally {
    loading.value = false;
  }
}

watch(articleRange, fetchArticleStats);
watch(userRange, fetchUserStats);
watch(categoryRange, fetchCategoryStats);
watch(myArticleRange, fetchMyArticleStats);

onMounted(() => {
  licenseStore.fetchLicenseInfo();
  load();
});
</script>

<style scoped>
.home-container {
  padding: 10px;
  width: 99%;
}
.tier-exceeded {
  color: #f56c6c;
  font-weight: 600;
}
.license-desc {
  margin-top: 8px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.range-switch {
  display: flex;
}
.chart-card :deep(.el-card__header) {
  min-height: 73px;
  box-sizing: border-box;
}
.card-subtitle {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.card-subtitle .vs {
  color: var(--el-color-success);
}
.card-subtitle .vs.negative {
  color: var(--el-color-danger);
}
</style>
