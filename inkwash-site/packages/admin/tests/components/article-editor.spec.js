import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { createArticle, updateArticle, commitArticle, resubmitArticle, getCategoriesTree, getTags, getArticle, uploadFile } =
  vi.hoisted(() => ({
    createArticle: vi.fn(),
    updateArticle: vi.fn(),
    commitArticle: vi.fn(),
    resubmitArticle: vi.fn(),
    getCategoriesTree: vi.fn(),
    getTags: vi.fn(),
    getArticle: vi.fn(),
    uploadFile: vi.fn(),
  }));

vi.mock('@inkwash/share', () => ({
  createArticle,
  updateArticle,
  commitArticle,
  resubmitArticle,
  getCategoriesTree,
  getTags,
  getArticle,
  uploadFile,
  getErrorMessage: (e, fallback = '') => fallback,
  MdEditor: {
    name: 'MdEditor',
    props: ['modelValue', 'height', 'toolbars', 'readonly', 'placeholder'],
    emits: ['update:modelValue', 'on-upload-img'],
    template: `<textarea class="md-stub" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />`,
  },
}));

import i18n from '@/locale/index';

import ArticleEditor from '@/components/ArticleEditor.vue';

const ElTreeSelectStub = {
  name: 'ElTreeSelect',
  props: ['modelValue', 'data', 'placeholder', 'clearable', 'filterable', 'disabled'],
  emits: ['update:modelValue'],
  template: `<input class="cat-stub" :value="modelValue ?? ''" @input="$emit('update:modelValue', Number($event.target.value))" />`,
};

function findButton(wrapper, label) {
  return wrapper.findAll('button').find((b) => b.text() === label);
}

async function mountEditor(articleId = null) {
  getCategoriesTree.mockResolvedValue([{ id: 1, name: '分类1' }]);
  getTags.mockResolvedValue({ list: [] });
  const wrapper = mount(ArticleEditor, {
    props: { articleId },
    global: {
      plugins: [ElementPlus, i18n],
      stubs: { ElTreeSelect: ElTreeSelectStub },
    },
  });
  await flushPromises();
  return wrapper;
}

async function fillValidForm(wrapper) {
  await wrapper.findAll('input')[0].setValue('标题');
  await wrapper.find('.cat-stub').setValue(1);
  await wrapper.find('.md-stub').setValue('# 正文');
  await flushPromises();
}

beforeEach(() => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe('admin ArticleEditor', () => {
  it('create mode "direct submit" saves the article and then submits it for review', async () => {
    createArticle.mockResolvedValue({ id: 42 });
    commitArticle.mockResolvedValue({ id: 42, status: 2 });

    const wrapper = await mountEditor(null);
    await fillValidForm(wrapper);

    await findButton(wrapper, '直接提交').trigger('click');
    await flushPromises();

    expect(createArticle).toHaveBeenCalledTimes(1);
    expect(commitArticle).toHaveBeenCalledWith(42);
    expect(wrapper.emitted('saved')).toBeTruthy();
  });

  it('edit mode REJECTED saves first then submits through the resubmit endpoint', async () => {
    getArticle.mockResolvedValue({
      id: 7,
      title: '旧标题',
      category: { id: 1 },
      content: '# 正文',
      status: 4,
      opinion: '需要修改',
    });
    resubmitArticle.mockResolvedValue({ id: 7, status: 2 });

    const wrapper = await mountEditor(7);
    await flushPromises();

    await findButton(wrapper, '重新提交').trigger('click');
    await flushPromises();

    expect(updateArticle).toHaveBeenCalledWith(7, expect.objectContaining({ title: '旧标题' }));
    expect(resubmitArticle).toHaveBeenCalledWith(7);
    expect(commitArticle).not.toHaveBeenCalled();
  });

  it('edit mode DRAFT saves first then submits through the commit endpoint', async () => {
    getArticle.mockResolvedValue({
      id: 8,
      title: '旧标题',
      category: { id: 1 },
      content: '# 正文',
      status: 1,
    });
    commitArticle.mockResolvedValue({ id: 8, status: 2 });

    const wrapper = await mountEditor(8);
    await flushPromises();

    await findButton(wrapper, '提交审核').trigger('click');
    await flushPromises();

    expect(updateArticle).toHaveBeenCalledWith(8, expect.objectContaining({ title: '旧标题' }));
    expect(commitArticle).toHaveBeenCalledWith(8);
    expect(resubmitArticle).not.toHaveBeenCalled();
  });

  it('create direct submit check-failure keeps the draft and retry does not duplicate', async () => {
    createArticle.mockResolvedValue({ id: 42 });
    commitArticle.mockRejectedValueOnce(new Error('敏感词')).mockResolvedValue({ id: 42, status: 2 });

    const wrapper = await mountEditor(null);
    await fillValidForm(wrapper);

    await findButton(wrapper, '直接提交').trigger('click');
    await flushPromises();

    expect(createArticle).toHaveBeenCalledTimes(1);
    expect(wrapper.emitted('saved')).toBeFalsy();

    await findButton(wrapper, '直接提交').trigger('click');
    await flushPromises();

    expect(createArticle).toHaveBeenCalledTimes(1);
    expect(updateArticle).toHaveBeenCalledWith(42, expect.any(Object));
    expect(commitArticle).toHaveBeenCalledTimes(2);
    expect(wrapper.emitted('saved')).toBeTruthy();
  });
});
