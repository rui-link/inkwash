import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createI18n } from 'vue-i18n';
import { createMemoryHistory, createRouter } from 'vue-router';

const {
  getPublicArticle,
  getCategoriesTree,
  getPublicTerms,
  createUserArticle,
  updateUserArticle,
  commitArticle,
  resubmitArticle,
  uploadCover,
  uploadMedia,
  getErrorMessage,
  ArticleStatus,
} = vi.hoisted(() => ({
  getPublicArticle: vi.fn(),
  getCategoriesTree: vi.fn(),
  getPublicTerms: vi.fn(),
  createUserArticle: vi.fn(),
  updateUserArticle: vi.fn(),
  commitArticle: vi.fn(),
  resubmitArticle: vi.fn(),
  uploadCover: vi.fn(),
  uploadMedia: vi.fn(),
  getErrorMessage: vi.fn(),
  ArticleStatus: { DRAFT: 1, PENDING: 2, APPROVED: 3, REJECTED: 4, PUBLISHED: 5, RETRACTED: 6 },
}));

vi.mock('@inkwash/share', () => ({
  getPublicArticle,
  getCategoriesTree,
  getPublicTerms,
  createUserArticle,
  updateUserArticle,
  commitArticle,
  resubmitArticle,
  uploadCover,
  uploadMedia,
  getErrorMessage,
  ArticleStatus,
}));

import ArticleEditor from '@/views/user/ArticleEditor.vue';

function articleFixture(status, extra = {}) {
  return {
    id: 9,
    title: '标题X',
    summary: '摘要X',
    coverUrl: '',
    category: null,
    terms: [],
    content: '# 内容X',
    status,
    opinion: '',
    ...extra,
  };
}

const MdEditorStub = {
  name: 'MdEditor',
  props: ['modelValue', 'height', 'toolbars', 'placeholder'],
  emits: ['update:modelValue', 'on-upload-img'],
  template: `<textarea class="md-stub" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
};

async function mountEditor({ url = '/articles/9/edit', article = articleFixture(1) } = {}) {
  getPublicArticle.mockResolvedValue(article);
  getCategoriesTree.mockResolvedValue([]);
  getPublicTerms.mockResolvedValue({ list: [] });
  createUserArticle.mockResolvedValue({ id: 42 });
  updateUserArticle.mockResolvedValue({});
  commitArticle.mockResolvedValue({});
  resubmitArticle.mockResolvedValue({});
  getErrorMessage.mockImplementation((e, fallback = '') => fallback);

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/articles/new', name: 'ArticleNew', component: { template: '<div />' } },
      { path: '/articles/:id/edit', name: 'ArticleEdit', component: { template: '<div />' } },
      { path: '/my-articles', name: 'MyArticles', component: { template: '<div />' } },
    ],
  });
  router.push(url);
  await router.isReady();

  const i18n = createI18n({
    legacy: false,
    locale: 'zh-CN',
    fallbackLocale: 'zh-CN',
    messages: {
      'zh-CN': {
        route: { editArticle: '编辑文章', writeArticle: '写文章' },
        article: {
          title: '标题',
          summary: '摘要',
          cover: '封面',
          category: '分类',
          tags: '标签',
          content: '内容',
          rejectOpinion: '驳回原因',
          draft: '保存草稿',
          submitReview: '提交审核',
          submitDirectly: '直接提交',
          resubmit: '重新提交',
          updateSuccess: '更新成功',
          committed: '已提交审核',
          resubmitSuccess: '已重新提交',
          fetchFailed: '加载失败',
          saveDraftSuccess: '草稿保存成功',
          submitReviewSuccess: '提交审核成功',
          markdownPlaceholder: '输入内容',
          selectCategory: '选择分类',
          selectTags: '选择标签',
          uploadCover: '上传封面',
          uploadAudio: '上传音频',
          uploadVideo: '上传视频',
          uploadMedia: '上传素材',
        },
        common: {
          cancel: '取消',
          delete: '删除',
          uploadSuccess: '上传成功',
          uploadFailed: '上传失败',
          operationFailed: '操作失败',
        },
      },
    },
  });

  const wrapper = mount(ArticleEditor, {
    global: { plugins: [ElementPlus, router, i18n], stubs: { MdEditor: MdEditorStub } },
  });
  await flushPromises();
  return { wrapper, router };
}

const findButton = (wrapper, label) => wrapper.findAll('button').find((b) => b.text() === label);

async function fillCreateForm(wrapper) {
  await wrapper.find('input').setValue('标题X');
  await wrapper.find('.md-stub').setValue('# 内容X');
  await flushPromises();
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('web article editor page', () => {
  it('rejected article submit saves edits then calls resubmitArticle', async () => {
    const { wrapper } = await mountEditor({ article: articleFixture(4, { opinion: '需要修改' }) });

    const submitBtn = findButton(wrapper, '重新提交');
    expect(submitBtn).toBeTruthy();

    await submitBtn.trigger('click');
    await flushPromises();

    expect(updateUserArticle).toHaveBeenCalledWith('9', expect.objectContaining({ title: '标题X' }));
    expect(resubmitArticle).toHaveBeenCalledWith('9');
    expect(commitArticle).not.toHaveBeenCalled();
  });

  it('draft article submit saves edits then calls commitArticle', async () => {
    const { wrapper } = await mountEditor({ article: articleFixture(1) });

    const submitBtn = findButton(wrapper, '提交审核');
    await submitBtn.trigger('click');
    await flushPromises();

    expect(updateUserArticle).toHaveBeenCalledWith('9', expect.objectContaining({ title: '标题X' }));
    expect(commitArticle).toHaveBeenCalledWith('9');
    expect(resubmitArticle).not.toHaveBeenCalled();
  });

  it('draft button on edit mode saves without submitting', async () => {
    const { wrapper } = await mountEditor({ article: articleFixture(1) });

    const draftBtn = findButton(wrapper, '保存草稿');
    await draftBtn.trigger('click');
    await flushPromises();

    expect(updateUserArticle).toHaveBeenCalledWith('9', expect.objectContaining({ title: '标题X' }));
    expect(commitArticle).not.toHaveBeenCalled();
    expect(resubmitArticle).not.toHaveBeenCalled();
  });

  it('create mode offers save-draft and direct-submit buttons', async () => {
    const { wrapper } = await mountEditor({ url: '/articles/new', article: null });

    expect(findButton(wrapper, '保存草稿')).toBeTruthy();
    expect(findButton(wrapper, '直接提交')).toBeTruthy();
  });

  it('create direct submit creates the article then submits it', async () => {
    const { wrapper, router } = await mountEditor({ url: '/articles/new', article: null });
    await fillCreateForm(wrapper);

    await findButton(wrapper, '直接提交').trigger('click');
    await flushPromises();

    expect(createUserArticle).toHaveBeenCalledWith(expect.objectContaining({ title: '标题X' }));
    expect(commitArticle).toHaveBeenCalledWith(42);
    expect(router.currentRoute.value.name).toBe('MyArticles');
  });

  it('create direct submit check-failure keeps the draft and retry does not duplicate', async () => {
    commitArticle.mockRejectedValueOnce(new Error('敏感词')).mockResolvedValue({});
    const { wrapper, router } = await mountEditor({ url: '/articles/new', article: null });
    await fillCreateForm(wrapper);

    await findButton(wrapper, '直接提交').trigger('click');
    await flushPromises();

    expect(createUserArticle).toHaveBeenCalledTimes(1);
    expect(router.currentRoute.value.name).toBe('ArticleNew');

    await findButton(wrapper, '直接提交').trigger('click');
    await flushPromises();

    expect(createUserArticle).toHaveBeenCalledTimes(1);
    expect(updateUserArticle).toHaveBeenCalledWith(42, expect.objectContaining({ title: '标题X' }));
    expect(commitArticle).toHaveBeenCalledTimes(2);
    expect(router.currentRoute.value.name).toBe('MyArticles');
  });

  it('rejected edit hides the save-draft button and only offers resubmit', async () => {
    const { wrapper } = await mountEditor({ article: articleFixture(4, { opinion: '需要修改' }) });

    expect(findButton(wrapper, '保存草稿')).toBeUndefined();
    expect(findButton(wrapper, '重新提交')).toBeTruthy();
  });
});
