import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createI18n } from 'vue-i18n';
import { createRouter, createMemoryHistory } from 'vue-router';

const { getPublicArticle, getPublicComments, addComment, ArticleStatus } = vi.hoisted(() => ({
  getPublicArticle: vi.fn(),
  getPublicComments: vi.fn(),
  addComment: vi.fn(),
  ArticleStatus: { DRAFT: 1, PENDING: 2, APPROVED: 3, REJECTED: 4, PUBLISHED: 5, RETRACTED: 6 },
}));

vi.mock('@inkwash/share', () => ({
  getPublicArticle,
  getPublicComments,
  addComment,
  ArticleStatus,
  useAuthStore: vi.fn(),
  renderMarkdown: (s) => s,
  getErrorMessage: (e) => e?.message || '',
  formatDate: (d) => d,
}));

import { useAuthStore } from '@inkwash/share';

import ArticleDetail from '@/views/ArticleDetail.vue';

const detailFixture = {
  id: 5,
  title: '详情文章',
  author: '王五',
  status: ArticleStatus.PUBLISHED,
  summary: '详情摘要',
  content: '# 墨水标题',
  coverUrl: 'https://cdn.example.com/cover.png',
  publishTime: '2026-01-03 08:00:00',
  terms: [{ id: 1, name: '标签一' }],
  tally: {
    commentCount: 2,
    agreeCount: 0,
    averseCount: 0,
    favoriteCount: 0,
    shareCount: 0,
  },
};

const InteractionBarStub = {
  name: 'InteractionBarStub',
  template: '<div class="interaction-stub" />',
};

const CommentItemStub = {
  name: 'CommentItemStub',
  props: ['comment'],
  emits: ['reply'],
  template: '<div class="comment-stub" @click="$emit(\'reply\', comment)">{{ comment.id }}</div>',
};

const CommentFormStub = {
  name: 'CommentFormStub',
  props: ['replyTo'],
  emits: ['submit', 'cancelReply'],
  template: '<div class="comment-form-stub" />',
};

const detailStubs = {
  InteractionBar: InteractionBarStub,
  CommentItem: CommentItemStub,
  CommentForm: CommentFormStub,
};

async function createDetailRouter(url = '/article/5') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/article/:id', component: { template: '<div />' } },
    ],
  });
  router.push(url);
  await router.isReady();
  return router;
}

function createDetailI18n() {
  return createI18n({
    legacy: false,
    locale: 'zh-CN',
    fallbackLocale: 'zh-CN',
    messages: {
      'zh-CN': {
        articleDetail: {
          notFound: '文章不存在',
          comments: '评论',
          signInToComment: '登录后评论',
          noComments: '暂无评论',
        },
        commentForm: { submitFailed: '评论失败' },
      },
    },
  });
}

async function mountDetail({
  url = '/article/5',
  article = detailFixture,
  comments = [],
  loggedIn = true,
  articleError = null,
} = {}) {
  const pinia = createPinia();

  const authState = loggedIn ? { token: 'test-token', isLoggedIn: true } : { token: '', isLoggedIn: false };
  vi.mocked(useAuthStore).mockReturnValue(authState);

  if (articleError) {
    getPublicArticle.mockRejectedValue(articleError);
  } else if (article !== null) {
    getPublicArticle.mockResolvedValue(article);
  }
  getPublicComments.mockResolvedValue(comments);

  const router = await createDetailRouter(url);
  const i18n = createDetailI18n();
  const wrapper = mount(ArticleDetail, {
    global: { plugins: [ElementPlus, router, i18n, pinia], stubs: detailStubs },
  });
  await flushPromises();
  return { wrapper, router };
}

beforeEach(() => {
  vi.clearAllMocks();
  if (!Element.prototype.scrollIntoView) {
    Element.prototype.scrollIntoView = vi.fn();
  }
});

describe('web article detail page', () => {
  it('renders skeleton while loading', async () => {
    let resolveArticle;
    getPublicArticle.mockReturnValue(new Promise((resolve) => (resolveArticle = resolve)));

    const { wrapper } = await mountDetail({ article: null });

    expect(wrapper.find('.loading-state').exists()).toBe(true);
    resolveArticle(detailFixture);
    await flushPromises();
  });

  it('renders header, tags, cover image and markdown content', async () => {
    const { wrapper } = await mountDetail();

    expect(wrapper.find('.article-title').text()).toBe('详情文章');
    expect(wrapper.text()).toContain('王五');
    expect(wrapper.find('.article-tag').text()).toBe('标签一');
    expect(wrapper.find('.article-cover img').attributes('src')).toBe('https://cdn.example.com/cover.png');
    expect(wrapper.find('.md-content').html()).toContain('# 墨水标题');
    expect(wrapper.find('.interaction-stub').exists()).toBe(true);
  });

  it('hides the cover block when the article has no cover', async () => {
    const { wrapper } = await mountDetail({
      article: { ...detailFixture, coverUrl: '' },
    });

    expect(wrapper.find('.article-cover').exists()).toBe(false);
  });

  it('shows the not-found empty state when fetching fails', async () => {
    const { wrapper } = await mountDetail({ articleError: new Error('gone') });

    expect(wrapper.find('.empty-state').exists()).toBe(true);
    expect(wrapper.find('.interaction-stub').exists()).toBe(false);
  });

  it('does not request comments for an unpublished article and hides the comment section', async () => {
    // Backend CommentServiceImpl.getComments returns 403 for any non-published article
    // (deliberate: otherwise comments of drafts could be enumerated by id), and the http
    // interceptor turns any API 403 into a redirect to /403. So requesting comments here
    // would bounce the author off their own article preview.
    const { wrapper } = await mountDetail({
      article: { ...detailFixture, status: ArticleStatus.DRAFT },
    });

    expect(getPublicComments).not.toHaveBeenCalled();
    expect(wrapper.find('.comment-section').exists()).toBe(false);
    expect(wrapper.find('.article-title').text()).toBe('详情文章');
  });

  it('renders loaded comments', async () => {
    const comments = [{ id: 11 }, { id: 22 }];
    const { wrapper } = await mountDetail({ comments });

    expect(wrapper.findAll('.comment-stub')).toHaveLength(2);
    expect(wrapper.text()).toContain('11');
  });

  it('shows login hint when logged out and form when logged in', async () => {
    const out = await mountDetail({ loggedIn: false });
    expect(out.wrapper.find('.login-hint').exists()).toBe(true);
    expect(out.wrapper.findComponent(CommentFormStub).exists()).toBe(false);

    const inside = await mountDetail({ loggedIn: true });
    expect(inside.wrapper.find('.login-hint').exists()).toBe(false);
    expect(inside.wrapper.findComponent(CommentFormStub).exists()).toBe(true);
  });

  it('submitting a comment posts it, refreshes comments and bumps the counter', async () => {
    const { wrapper } = await mountDetail({
      comments: [{ id: 11 }],
    });
    addComment.mockResolvedValue({});
    getPublicComments.mockResolvedValue([{ id: 11 }, { id: 12 }]);
    const done = vi.fn();

    wrapper.findComponent(CommentFormStub).vm.$emit('submit', {
      content: '好文!',
      parentId: null,
      done,
    });
    await flushPromises();

    expect(addComment).toHaveBeenCalledWith(5, {
      content: '好文!',
      articleId: 5,
      parentId: null,
    });
    expect(done).toHaveBeenCalledWith();
    expect(wrapper.findAll('.comment-stub')).toHaveLength(2);
    expect(wrapper.find('.comment-count').text()).toBe('3');
  });

  it('failed comment submit passes the error message to done', async () => {
    const { wrapper } = await mountDetail();
    addComment.mockRejectedValue(new Error('太快了'));
    const done = vi.fn();

    wrapper.findComponent(CommentFormStub).vm.$emit('submit', {
      content: '好文!',
      parentId: null,
      done,
    });
    await flushPromises();

    expect(done).toHaveBeenCalledWith(expect.any(String));
    expect(getPublicComments).toHaveBeenCalledTimes(1);
  });

  it('reply sets replyTo and cancel-reply clears it', async () => {
    const comments = [{ id: 11 }];
    const { wrapper } = await mountDetail({ comments });
    const form = wrapper.findComponent(CommentFormStub);

    wrapper.findComponent(CommentItemStub).vm.$emit('reply', comments[0]);
    await flushPromises();
    expect(form.props('replyTo')).toEqual(comments[0]);

    form.vm.$emit('cancelReply');
    await flushPromises();
    expect(form.props('replyTo')).toBeNull();
  });
});
