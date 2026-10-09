import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import i18n from '@/locale/index';

const state = vi.hoisted(() => ({ roles: [] }));

const { getArticleStats, getUserStats, getCategoryStats, getDashboardSummary, getMyArticleStats, getCmsDashboard } =
  vi.hoisted(() => ({
    getArticleStats: vi.fn(),
    getUserStats: vi.fn(),
    getCategoryStats: vi.fn(),
    getDashboardSummary: vi.fn(),
    getMyArticleStats: vi.fn(),
    getCmsDashboard: vi.fn(),
  }));

vi.mock('@inkwash/share', () => ({
  getArticleStats,
  getUserStats,
  getCategoryStats,
  getDashboardSummary,
  getMyArticleStats,
  getCmsDashboard,
  useAuthStore: () => ({
    get roles() {
      return state.roles;
    },
  }),
  useLicenseStore: () => ({
    holder: 'InkWash',
    edition: 'enterprise',
    currentUserCount: 5,
    maxUsers: 100,
    restrictedModules: [],
    tierExceeded: false,
    fetchLicenseInfo: () => {},
  }),
}));

vi.mock('vue-echarts', () => ({
  default: { name: 'v-chart', props: ['option'], template: '<div />' },
}));

import HomePage from '@/views/home/index.vue';

function statsPayload() {
  getArticleStats.mockResolvedValue([
    {
      timeAxis: '2026-09-01',
      created: 10,
      pending: 3,
      approved: 4,
      rejected: 1,
      pendingPublish: 4,
      published: 6,
    },
  ]);
  getUserStats.mockResolvedValue([{ timeAxis: '2026-09-01', newUsers: 2, activeAuthors: 1 }]);
  getCategoryStats.mockResolvedValue([
    {
      timeAxis: '2026-09-01',
      categories: [{ categoryId: 1, categoryName: '技术', count: 4 }],
    },
  ]);
  getDashboardSummary.mockResolvedValue({
    totalArticles: 10,
    pendingReview: 3,
    approved: 4,
    rejected: 1,
    published: 6,
    totalUsers: 8,
  });
  getMyArticleStats.mockResolvedValue({
    items: [
      {
        timeAxis: '2026-09-01',
        pending: 3,
        approved: 4,
        rejected: 1,
        published: 6,
      },
      {
        timeAxis: '2026-09-02',
        pending: 0,
        approved: 1,
        rejected: 0,
        published: 2,
      },
    ],
    prevCreated: 8,
  });
  getCmsDashboard.mockResolvedValue({
    myArticles: 14,
    myComments: 5,
    myFavorites: 3,
    myAgrees: 2,
  });
}

async function mountHome() {
  const wrapper = mount(HomePage, {
    attachTo: document.body,
    global: { plugins: [ElementPlus, i18n] },
  });
  await flushPromises();
  return wrapper;
}

beforeEach(() => {
  state.roles = [];
  vi.clearAllMocks();
});

describe('admin home dashboard', () => {
  it('admin 渲染统计卡片与两类图表，并请求近7天数据', async () => {
    state.roles = ['ROLE_SYSTEM'];
    statsPayload();
    const wrapper = await mountHome();

    expect(getDashboardSummary).toHaveBeenCalledOnce();
    expect(getArticleStats).toHaveBeenCalledWith('last7d');
    expect(getUserStats).toHaveBeenCalledWith('last7d');
    expect(getCategoryStats).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('文章总数');
    expect(wrapper.text()).toContain('文章数统计');
    expect(wrapper.text()).toContain('用户数统计');
    expect(wrapper.findAllComponents({ name: 'v-chart' })).toHaveLength(2);
  });

  it('admin 切换文章图表为近12个月时单独重请求文章统计', async () => {
    state.roles = ['ROLE_ADMIN'];
    statsPayload();
    const wrapper = await mountHome();

    const last12mBtn = wrapper.findAll('.el-radio-button').find((b) => b.text() === '近12个月');
    await last12mBtn.trigger('click');
    await flushPromises();

    expect(getArticleStats).toHaveBeenLastCalledWith('last12m');
    expect(getUserStats).toHaveBeenCalledTimes(1);
  });

  it('editor 渲染文章数统计与文章类型统计，不请求用户统计', async () => {
    state.roles = ['ROLE_EDITOR'];
    statsPayload();
    const wrapper = await mountHome();

    expect(getArticleStats).toHaveBeenCalledWith('last7d');
    expect(getCategoryStats).toHaveBeenCalledWith('last7d');
    expect(getUserStats).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('文章类型统计');
  });

  it('普通用户渲染我的文章统计与互动统计，并拉取自身数据', async () => {
    state.roles = ['ROLE_USER'];
    statsPayload();
    const wrapper = await mountHome();

    expect(getMyArticleStats).toHaveBeenCalledWith('thisMonth');
    expect(getCmsDashboard).toHaveBeenCalledOnce();
    expect(getArticleStats).not.toHaveBeenCalled();
    expect(getUserStats).not.toHaveBeenCalled();
    expect(getCategoryStats).not.toHaveBeenCalled();
    expect(getDashboardSummary).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('我的文章统计');
    expect(wrapper.text()).toContain('我的互动统计');
    expect(wrapper.findAllComponents({ name: 'v-chart' })).toHaveLength(2);

    const myChart = wrapper.findAllComponents({ name: 'v-chart' }).find((c) => c.props('option').xAxis);
    expect(myChart.props('option').xAxis.data).toEqual(['09-01', '09-02']);
    expect(wrapper.text()).toContain('较上月');
  });

  it('普通用户切换我的文章统计为上月时单独重请求', async () => {
    state.roles = ['ROLE_USER'];
    statsPayload();
    const wrapper = await mountHome();

    const lastMonthBtn = wrapper.findAll('.el-radio-button').find((b) => b.text() === '上月');
    await lastMonthBtn.trigger('click');
    await flushPromises();

    expect(getMyArticleStats).toHaveBeenLastCalledWith('lastMonth');
    expect(getCmsDashboard).toHaveBeenCalledTimes(1);
  });
});
