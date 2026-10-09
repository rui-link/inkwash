import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { articleApi } = vi.hoisted(() => ({
  articleApi: {
    getArticle: vi.fn(),
    reviewArticle: vi.fn(),
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
  renderMarkdown: (c) => `<h1>${c}</h1>`,
}));

import ReviewPage from '@/views/content/article/review.vue';

import { createTestRouter, mountPage, spyMessages } from '../../../helpers/mount-page';

const pendingArticle = {
  id: 12,
  title: '待审文章',
  author: '李四',
  category: { name: '随笔' },
  status: 2,
  summary: '待审摘要',
  content: '# 待审正文',
  createTime: '2026-01-02 09:00:00',
};

async function mountReview(query = '?id=12', article = pendingArticle) {
  if (article) {
    vi.spyOn(articleApi, 'getArticle').mockResolvedValue(article);
  } else {
    vi.spyOn(articleApi, 'getArticle').mockRejectedValue(new Error('gone'));
  }
  const router = await createTestRouter('/cms/article/review' + query);
  const wrapper = mountPage(ReviewPage, router);
  await flushPromises();
  return { wrapper, router };
}

async function selectRadio(wrapper, index) {
  const radioGroup = wrapper.findComponent({ name: 'ElRadioGroup' });
  const value = index === 0 ? true : radioGroup.findAllComponents({ name: 'ElRadio' })[index].props('value');
  await radioGroup.setValue(value);
}

beforeEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe('admin article review page', () => {
  it('missing id query redirects without fetching', async () => {
    spyMessages();
    const { router } = await mountReview('');

    expect(articleApi.getArticle).not.toHaveBeenCalled();
    expect(router.currentRoute.value.path).toBe('/cms/article');
  });

  it('fetch failure shows an error and redirects', async () => {
    const messages = spyMessages();
    const { router } = await mountReview('?id=404', null);

    expect(messages.error).toHaveBeenCalled();
    expect(router.currentRoute.value.path).toBe('/cms/article');
  });

  it('renders article preview for a pending article', async () => {
    const { wrapper } = await mountReview();

    expect(wrapper.text()).toContain('待审文章');
    expect(wrapper.text()).toContain('李四');
    expect(wrapper.find('.review-panel').exists()).toBe(true);
  });

  it('hides the review panel when article is not pending', async () => {
    const { wrapper } = await mountReview('?id=12', {
      ...pendingArticle,
      status: 3,
    });

    expect(wrapper.find('.review-panel').exists()).toBe(false);
  });

  it('submit without a decision is blocked with a warning', async () => {
    vi.spyOn(articleApi, 'reviewArticle').mockResolvedValue({});
    const messages = spyMessages();
    const { wrapper, router } = await mountReview();

    const submitBtn = wrapper.findAll('button').find((b) => b.text() === '确定');
    await submitBtn.trigger('click');
    await flushPromises();

    expect(messages.warning).toHaveBeenCalled();
    expect(articleApi.reviewArticle).not.toHaveBeenCalled();
    expect(router.currentRoute.value.fullPath).toBe('/cms/article/review?id=12');
  });

  it('approve submits approved payload and redirects', async () => {
    vi.spyOn(articleApi, 'reviewArticle').mockResolvedValue({});
    const messages = spyMessages();
    const { wrapper, router } = await mountReview();

    await selectRadio(wrapper, 0);
    const submitBtn = wrapper.findAll('button').find((b) => b.text() === '确定');
    await submitBtn.trigger('click');
    await flushPromises();

    expect(articleApi.reviewArticle).toHaveBeenCalledWith(12, {
      approved: true,
      opinion: '',
    });
    expect(messages.success).toHaveBeenCalled();
    expect(router.currentRoute.value.path).toBe('/cms/article');
  });

  it('reject without opinion is blocked with a warning', async () => {
    vi.spyOn(articleApi, 'reviewArticle').mockResolvedValue({});
    const messages = spyMessages();
    const { wrapper } = await mountReview();

    await selectRadio(wrapper, 1);
    const submitBtn = wrapper.findAll('button').find((b) => b.text() === '确定');
    await submitBtn.trigger('click');
    await flushPromises();

    expect(messages.warning).toHaveBeenCalled();
    expect(articleApi.reviewArticle).not.toHaveBeenCalled();
  });

  it('reject with opinion submits the typed opinion and redirects', async () => {
    vi.spyOn(articleApi, 'reviewArticle').mockResolvedValue({});
    spyMessages();
    const { wrapper, router } = await mountReview();

    await selectRadio(wrapper, 1);
    await wrapper.find('.reject-reason textarea').setValue('论据不足');
    const submitBtn = wrapper.findAll('button').find((b) => b.text() === '确定');
    await submitBtn.trigger('click');
    await flushPromises();

    expect(articleApi.reviewArticle).toHaveBeenCalledWith(12, {
      approved: false,
      opinion: '论据不足',
    });
    expect(router.currentRoute.value.path).toBe('/cms/article');
  });
});
