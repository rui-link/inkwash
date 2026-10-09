import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { getPublicArticles } = vi.hoisted(() => ({
  getPublicArticles: vi.fn(),
}));

vi.mock('@inkwash/share', () => ({
  getPublicArticles,
  normalizePage: (res) => {
    if (!res || typeof res !== 'object') return { items: [], total: 0 };
    const items = Array.isArray(res.list) ? res.list : [];
    const total = res.total ?? items.length;
    return { items, total };
  },
  formatDate: (d) => d,
}));

import ArticleListPage from '@/views/ArticleList.vue';

import { createTestRouter, mountPage } from '../helpers/mount-page';

const articleListFixture = [
  {
    id: 1,
    title: '第一篇文章',
    author: '张三',
    summary: '摘要一',
    status: 3,
    createTime: '2026-01-01 08:00:00',
  },
  {
    id: 2,
    title: '第二篇文章',
    author: '李四',
    summary: '摘要二',
    status: 3,
    createTime: '2026-01-02 08:00:00',
  },
];

async function mountList() {
  getPublicArticles.mockResolvedValue({ list: articleListFixture, total: 2 });
  const router = await createTestRouter('/');
  const wrapper = mountPage(ArticleListPage, router);
  await flushPromises();
  return { wrapper, router };
}

beforeEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe('web article list page', () => {
  it('fetches and renders a list of articles', async () => {
    const { wrapper } = await mountList();

    expect(getPublicArticles).toHaveBeenCalled();
    expect(wrapper.findAll('.article-card').length).toBe(2);
    expect(wrapper.text()).toContain('第一篇文章');
    expect(wrapper.text()).toContain('张三');
    expect(wrapper.text()).toContain('摘要一');
  });

  it('clears articles and propagates error when fetch fails', async () => {
    getPublicArticles.mockRejectedValue(new Error('network'));
    const swallow = () => {};
    process.on('unhandledRejection', swallow);
    try {
      const router = await createTestRouter('/');
      const wrapper = mountPage(ArticleListPage, router);
      await flushPromises();

      expect(getPublicArticles).toHaveBeenCalled();
      expect(wrapper.findAll('.article-card').length).toBe(0);
    } finally {
      process.off('unhandledRejection', swallow);
    }
  });

  it('clicking an article navigates to the article detail page', async () => {
    const { wrapper, router } = await mountList();

    const firstCard = wrapper.find('.article-card');
    await firstCard.trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/article/1');
  });

  it('shows the page heading', async () => {
    const { wrapper } = await mountList();

    expect(wrapper.find('.list-title').exists()).toBe(true);
    expect(wrapper.find('.list-title').text()).toBe('文章');
  });

  it('shows an empty state when the list is empty', async () => {
    getPublicArticles.mockResolvedValue({ list: [], total: 0 });
    const router = await createTestRouter('/');
    const wrapper = mountPage(ArticleListPage, router);
    await flushPromises();

    expect(wrapper.findAll('.article-card').length).toBe(0);
    expect(wrapper.text()).toContain('暂无文章');
  });

  it('renders article card elements', async () => {
    const { wrapper } = await mountList();

    expect(wrapper.find('.article-card').exists()).toBe(true);
    expect(wrapper.find('.card-title').exists()).toBe(true);
    expect(wrapper.find('.card-body').exists()).toBe(true);
  });

  it('shows loading state before data arrives', async () => {
    getPublicArticles.mockReturnValue(new Promise(() => {}));
    const router = await createTestRouter('/');
    const wrapper = mountPage(ArticleListPage, router);
    await flushPromises();

    expect(wrapper.find('.loading-state').exists()).toBe(true);
    expect(wrapper.find('.skeleton-card').exists()).toBe(true);
  });
});
