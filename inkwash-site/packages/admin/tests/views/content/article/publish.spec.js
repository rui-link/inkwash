import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { articleApi } = vi.hoisted(() => ({
  articleApi: {
    getArticle: vi.fn(),
    publishArticle: vi.fn(),
  },
}));

vi.mock('@inkwash/share', () => ({
  articleApi,
  getErrorMessage: (e, fallback = '') => fallback,
  ArticleStatus: {
    DRAFT: 1,
    PENDING: 2,
    APPROVED: 3,
    REJECTED: 4,
    PUBLISHED: 5,
    RETRACTED: 6,
  },
  ArticleStatusMap: {
    1: { label: '草稿', type: 'info' },
    2: { label: '待审核', type: 'warning' },
    3: { label: '通过', type: 'success' },
    4: { label: '驳回', type: 'danger' },
    5: { label: '已发布', type: 'info' },
    6: { label: '已撤回', type: 'info' },
  },
  ArticleStatusLabelKey: {
    1: 'statusDraft',
    2: 'statusPending',
    3: 'statusApproved',
    4: 'statusRejected',
    5: 'statusPublished',
    6: 'statusRetracted',
  },
  formatDate: (d) => d,
}));

import PublishPage from '@/views/content/article/publish.vue';

import { createTestRouter, mountPage, rejectConfirm, resolveConfirm, spyMessages } from '../../../helpers/mount-page';

const articleFixture = {
  id: 9,
  title: '发布测试文章',
  authorName: '张三',
  categoryName: '技术',
  status: 3,
  opinion: '',
  summary: '这是一篇摘要',
};

async function mountPublish() {
  const router = await createTestRouter({
    path: '/cms/article/publish',
    query: { id: '9' },
  });
  const wrapper = mountPage(PublishPage, router);
  await flushPromises();
  return { wrapper, router };
}

beforeEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe('admin article publish page', () => {
  it('fetches the article by route param and renders its fields', async () => {
    articleApi.getArticle.mockResolvedValue(articleFixture);

    const { wrapper } = await mountPublish();

    expect(articleApi.getArticle).toHaveBeenCalledWith('9');
    expect(wrapper.text()).toContain('发布测试文章');
    expect(wrapper.text()).toContain('张三');
    expect(wrapper.text()).toContain('技术');
    expect(wrapper.text()).toContain('这是一篇摘要');
    expect(wrapper.text()).toContain('无');
  });

  it('confirmed publish calls the API and redirects to the list', async () => {
    articleApi.getArticle.mockResolvedValue(articleFixture);
    articleApi.publishArticle.mockResolvedValue({});
    resolveConfirm('confirm');
    const messages = spyMessages();
    const { wrapper, router } = await mountPublish();

    const btn = wrapper.findAll('button').find((b) => b.text() === '发布');
    await btn.trigger('click');
    await flushPromises();

    expect(articleApi.publishArticle).toHaveBeenCalledWith('9');
    expect(router.currentRoute.value.path).toBe('/cms/article');
    expect(messages.success).toHaveBeenCalled();
  });

  it('cancelled confirm does not publish', async () => {
    articleApi.getArticle.mockResolvedValue(articleFixture);
    articleApi.publishArticle.mockResolvedValue({});
    rejectConfirm('cancel');
    spyMessages();
    const { wrapper, router } = await mountPublish();

    const btn = wrapper.findAll('button').find((b) => b.text() === '发布');
    await btn.trigger('click');
    await flushPromises();

    expect(articleApi.publishArticle).not.toHaveBeenCalled();
    expect(router.currentRoute.value.path).toBe('/cms/article/publish');
  });

  it('failed publish surfaces an error toast and stays on page', async () => {
    articleApi.getArticle.mockResolvedValue(articleFixture);
    articleApi.publishArticle.mockRejectedValue(new Error('boom'));
    resolveConfirm('confirm');
    const messages = spyMessages();
    const { wrapper, router } = await mountPublish();

    const btn = wrapper.findAll('button').find((b) => b.text() === '发布');
    await btn.trigger('click');
    await flushPromises();

    expect(messages.error).toHaveBeenCalled();
    expect(router.currentRoute.value.path).toBe('/cms/article/publish');
  });

  it('cancel button navigates back without any API write', async () => {
    articleApi.getArticle.mockResolvedValue(articleFixture);
    articleApi.publishArticle.mockResolvedValue({});
    const { wrapper, router } = await mountPublish();

    const cancelBtn = wrapper.findAll('button').find((b) => b.text() === '取消');
    await cancelBtn.trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/cms/article');
    expect(articleApi.publishArticle).not.toHaveBeenCalled();
  });
});
