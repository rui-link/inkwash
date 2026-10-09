import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createI18n } from 'vue-i18n';

const { getMyFavorites, getMyAgreements, getMyDislikes, getMyCommented } = vi.hoisted(() => ({
  getMyFavorites: vi.fn(),
  getMyAgreements: vi.fn(),
  getMyDislikes: vi.fn(),
  getMyCommented: vi.fn(),
}));

vi.mock('@inkwash/share', () => ({
  getMyFavorites,
  getMyAgreements,
  getMyDislikes,
  getMyCommented,
  normalizePage: (res) => {
    if (!res || typeof res !== 'object') return { items: [], total: 0 };
    const items = Array.isArray(res.list) ? res.list : [];
    const rawTotal = res.total ?? items.length;
    const total = Number.isFinite(Number(rawTotal)) && rawTotal !== '' ? Number(rawTotal) : items.length;
    return { items, total };
  },
  getErrorMessage: (e) => e?.message || '',
}));

import MyInteractions from '@/views/user/MyInteractions.vue';

const ArticleCardStub = {
  name: 'ArticleCard',
  template: '<div class="card-stub" />',
};

function createI18nDict() {
  return createI18n({
    legacy: false,
    locale: 'zh-CN',
    fallbackLocale: 'zh-CN',
    messages: {
      'zh-CN': {
        myInteractions: {
          title: '我的互动',
          desc: '浏览您的互动记录',
          commented: '已评论',
          agreed: '已赞同',
          aversed: '已反对',
          favorited: '已收藏',
          noData: '暂无数据',
          failed: '拉取互动失败',
          loadMore: '加载更多',
        },
      },
    },
  });
}

async function mountPage() {
  const pinia = createPinia();
  const i18n = createI18nDict();
  const wrapper = mount(MyInteractions, {
    global: {
      plugins: [ElementPlus, i18n, pinia],
      stubs: { ArticleCard: ArticleCardStub },
    },
  });
  await flushPromises();
  return wrapper;
}

beforeEach(() => {
  vi.resetAllMocks();
});

describe('web my-interactions paging view', () => {
  it('loads the commented tab first and shows load-more when more pages exist', async () => {
    getMyCommented.mockResolvedValue({ list: [{ id: 1 }], total: 25 });
    const wrapper = await mountPage();

    expect(getMyCommented).toHaveBeenCalledWith({ page: 1, size: 20 });
    expect(wrapper.findAll('.card-stub')).toHaveLength(1);
    expect(wrapper.find('.load-more').exists()).toBe(true);
  });

  it('appends the next page on load-more click', async () => {
    getMyCommented
      .mockResolvedValueOnce({ list: [{ id: 1 }], total: 25 })
      .mockResolvedValueOnce({ list: [{ id: 2 }], total: 25 });
    const wrapper = await mountPage();

    await wrapper.find('.load-more').trigger('click');
    await flushPromises();

    expect(getMyCommented).toHaveBeenLastCalledWith({ page: 2, size: 20 });
    expect(wrapper.findAll('.card-stub')).toHaveLength(2);
  });

  it('hides load-more once the list is exhausted', async () => {
    getMyCommented.mockResolvedValue({ list: [{ id: 1 }], total: 1 });
    const wrapper = await mountPage();

    expect(wrapper.find('.load-more').exists()).toBe(false);
  });

  it('shows the empty state and hides load-more when there are no records', async () => {
    getMyCommented.mockResolvedValue({ list: [], total: 0 });
    const wrapper = await mountPage();

    expect(wrapper.find('.empty-state').exists()).toBe(true);
    expect(wrapper.find('.load-more').exists()).toBe(false);
  });
});
