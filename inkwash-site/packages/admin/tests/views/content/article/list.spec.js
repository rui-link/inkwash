import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { articleApi, categoryApi } = vi.hoisted(() => ({
  articleApi: {
    listAllArticles: vi.fn(),
    getArticle: vi.fn(),
    deleteArticle: vi.fn(),
    commitArticle: vi.fn(),
    resubmitArticle: vi.fn(),
    reviewArticle: vi.fn(),
    publishArticle: vi.fn(),
    retractArticle: vi.fn(),
  },
  categoryApi: {
    getCategoryTree: vi.fn(),
  },
}));

vi.mock('@inkwash/share', () => ({
  articleApi,
  categoryApi,
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

import Pagination from '@/components/Pagination/index.vue';
import ArticleListPage from '@/views/content/article/index.vue';

import { createTestRouter, mountPage, rejectConfirm, resolveConfirm } from '../../../helpers/mount-page';

function row(id, status, extra = {}) {
  return {
    id,
    title: `文章${id}`,
    author: `作者${id}`,
    category: { name: `分类${id}` },
    status,
    opinion: '',
    createTime: '2026-01-01 10:00:00',
    content: '# 正文标题',
    ...extra,
  };
}

async function mountList(rows, total = rows.length) {
  articleApi.listAllArticles.mockResolvedValue({ list: rows, total });
  categoryApi.getCategoryTree.mockResolvedValue([]);
  const router = await createTestRouter('/cms/article');
  const wrapper = mountPage(ArticleListPage, router);
  await flushPromises();
  return { wrapper, router };
}

const rowButtons = (tr) => tr.findAll('button').map((b) => b.text());

beforeEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe('admin article list page', () => {
  it('renders table rows and total from the API', async () => {
    const rows = [row(1, 1), row(2, 2)];
    const { wrapper } = await mountList(rows, 99);

    expect(wrapper.findAll('.el-table__row')).toHaveLength(2);
    expect(wrapper.text()).toContain('文章1');
    expect(wrapper.findComponent(Pagination).props('total')).toBe(99);
  });

  it('search resets page to 1 and passes entered filters', async () => {
    const { wrapper } = await mountList([row(1, 1)]);

    await wrapper.find('.search-container input').setValue('ink');
    const searchBtn = wrapper.findAll('button').find((b) => b.text() === '搜索');
    await searchBtn.trigger('click');
    await flushPromises();

    expect(articleApi.listAllArticles).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, title: 'ink' }));
  });

  it('reset clears filters and refetches', async () => {
    const { wrapper } = await mountList([row(1, 1)]);

    await wrapper.find('.search-container input').setValue('ink');
    const resetBtn = wrapper.findAll('button').find((b) => b.text() === '重置');
    await resetBtn.trigger('click');
    await flushPromises();

    expect(articleApi.listAllArticles).toHaveBeenLastCalledWith(
      expect.objectContaining({
        page: 1,
        title: '',
        status: null,
        categoryId: null,
      }),
    );
  });

  it.each([
    [1, ['编辑', '提交审核', '删除']],
    [2, ['编辑', '预览', '审核', '删除']],
    [3, ['编辑', '预览', '发布', '删除']],
    [4, ['编辑', '重新提交', '删除']],
    [5, ['编辑', '预览', '撤回', '删除']],
  ])('shows status-driven action buttons for status %i', async (status, expected) => {
    const { wrapper } = await mountList([row(9, status)]);

    const tr = wrapper.find('.el-table__row');
    expect(rowButtons(tr)).toEqual(expected);
  });

  it('rejected row shows opinion tooltip on the status tag', async () => {
    const { wrapper } = await mountList([row(9, 4, { opinion: '质量不行' })]);

    expect(wrapper.find('.el-table__row .el-tooltip__trigger').text()).toContain('已驳回');
  });

  it('delete asks confirmation, calls the API and refreshes', async () => {
    resolveConfirm('confirm');
    const { wrapper } = await mountList([row(7, 1)]);
    const callsBefore = articleApi.listAllArticles.mock.calls.length;

    const deleteBtn = wrapper
      .find('.el-table__row')
      .findAll('button')
      .find((b) => b.text() === '删除');
    await deleteBtn.trigger('click');
    await flushPromises();

    expect(articleApi.deleteArticle).toHaveBeenCalledWith(7);
    expect(articleApi.listAllArticles.mock.calls.length).toBe(callsBefore + 1);
  });

  it('cancelled delete does not call the API', async () => {
    rejectConfirm();
    const { wrapper } = await mountList([row(7, 1)]);
    const callsBefore = articleApi.listAllArticles.mock.calls.length;

    const deleteBtn = wrapper
      .find('.el-table__row')
      .findAll('button')
      .find((b) => b.text() === '删除');
    await deleteBtn.trigger('click');
    await flushPromises();

    expect(articleApi.deleteArticle).not.toHaveBeenCalled();
    expect(articleApi.listAllArticles.mock.calls.length).toBe(callsBefore);
  });

  it('preview fetches article detail and renders its markdown content', async () => {
    articleApi.getArticle.mockResolvedValue({
      id: 8,
      title: '文章8',
      author: '作者8',
      category: { name: '分类8' },
      status: 3,
      content: '# 正文标题',
    });
    const { wrapper } = await mountList([row(8, 3)]);

    const previewBtn = wrapper
      .find('.el-table__row')
      .findAll('button')
      .find((b) => b.text() === '预览');
    await previewBtn.trigger('click');
    await flushPromises();

    expect(articleApi.getArticle).toHaveBeenCalledWith(8);
    expect(wrapper.find('.preview-title').text()).toBe('文章8');
    expect(wrapper.find('.md-content, .markdown-preview').html()).toContain('# 正文标题');
  });

  it('commit flow confirms then refreshes the list', async () => {
    resolveConfirm('confirm');
    const { wrapper } = await mountList([row(6, 1)]);
    const callsBefore = articleApi.listAllArticles.mock.calls.length;

    const commitBtn = wrapper
      .find('.el-table__row')
      .findAll('button')
      .find((b) => b.text() === '提交审核');
    await commitBtn.trigger('click');
    await flushPromises();

    expect(articleApi.commitArticle).toHaveBeenCalledWith(6);
    expect(articleApi.listAllArticles.mock.calls.length).toBe(callsBefore + 1);
  });

  it('rejected row commit calls resubmitArticle instead of commitArticle', async () => {
    resolveConfirm('confirm');
    const { wrapper } = await mountList([row(13, 4)]);

    const resubmitBtn = wrapper
      .find('.el-table__row')
      .findAll('button')
      .find((b) => b.text() === '重新提交');
    await resubmitBtn.trigger('click');
    await flushPromises();

    expect(articleApi.resubmitArticle).toHaveBeenCalledWith(13);
    expect(articleApi.commitArticle).not.toHaveBeenCalled();
  });

  it('commit failure still refreshes the list', async () => {
    resolveConfirm('confirm');
    articleApi.commitArticle.mockRejectedValue(new Error('boom'));
    const { wrapper } = await mountList([row(12, 1)]);
    const callsBefore = articleApi.listAllArticles.mock.calls.length;

    const commitBtn = wrapper
      .find('.el-table__row')
      .findAll('button')
      .find((b) => b.text() === '提交审核');
    await commitBtn.trigger('click');
    await flushPromises();

    expect(articleApi.listAllArticles.mock.calls.length).toBe(callsBefore + 1);
  });

  it('re-entrant commit click fires a single API call', async () => {
    resolveConfirm('confirm');
    let resolveCommit;
    articleApi.commitArticle.mockReturnValue(
      new Promise((resolve) => {
        resolveCommit = resolve;
      }),
    );
    const { wrapper } = await mountList([row(11, 1)]);

    const commitBtn = wrapper
      .find('.el-table__row')
      .findAll('button')
      .find((b) => b.text() === '提交审核');
    await commitBtn.trigger('click');
    await flushPromises();
    await commitBtn.trigger('click');
    await flushPromises();
    resolveCommit();
    await flushPromises();

    expect(articleApi.commitArticle).toHaveBeenCalledTimes(1);
  });

  it('pagination event reloads with the chosen page', async () => {
    const { wrapper } = await mountList(
      Array.from({ length: 10 }, (_, i) => row(i + 1, 1)),
      25,
    );

    const pager = wrapper.findComponent(Pagination);
    pager.vm.$emit('pagination', { page: 3, limit: 10 });
    await flushPromises();

    expect(articleApi.listAllArticles).toHaveBeenLastCalledWith(expect.objectContaining({ page: 3, size: 10 }));
  });
});
