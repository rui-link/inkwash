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
  <div class="article-editor">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item :label="$t('article.title')" prop="title">
        <el-input
          v-model="form.title"
          :placeholder="$t('article.title')"
          maxlength="240"
          show-word-limit
          :disabled="isEdit && !canEdit" />
      </el-form-item>
      <el-form-item :label="$t('article.category')" prop="categoryId">
        <el-tree-select
          v-model="form.categoryId"
          :data="categories"
          :props="{ children: 'children', label: 'name' }"
          node-key="id"
          :placeholder="$t('article.selectCategory')"
          clearable
          filterable
          style="width: 300px"
          :disabled="isEdit && !canEdit" />
      </el-form-item>
      <el-form-item :label="$t('article.tags')">
        <el-select
          v-model="form.termIds"
          :placeholder="$t('article.selectTags')"
          multiple
          clearable
          filterable
          style="width: 300px"
          :disabled="isEdit && !canEdit">
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('article.summary')" prop="summary">
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="2"
          :placeholder="$t('article.summary')"
          maxlength="500"
          show-word-limit
          :disabled="isEdit && !canEdit" />
      </el-form-item>
      <el-form-item :label="$t('article.cover')" prop="coverUrl">
        <div class="cover-upload">
          <el-input v-model="form.coverUrl" :placeholder="$t('article.cover')" :disabled="isEdit && !canEdit" />
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            accept="image/*"
            :before-upload="handleUploadCover"
            :disabled="isEdit && !canEdit">
            <el-button :disabled="isEdit && !canEdit">{{ $t('profile.upload') }}</el-button>
          </el-upload>
        </div>
      </el-form-item>
      <el-form-item :label="$t('article.status')" prop="status" v-if="isEdit">
        <el-tag :type="statusTagType(form.status)">{{ $t(statusLabel(form.status)) }}</el-tag>
        <span v-if="!canEdit" class="readonly-hint">{{ $t('article.readonly') }}</span>
      </el-form-item>
      <el-form-item :label="$t('article.reviewOpinion')" v-if="reviewOpinion">
        <div class="review-opinion">{{ reviewOpinion }}</div>
      </el-form-item>
      <el-form-item :label="$t('article.content')" prop="content">
        <div class="editor-wrapper">
          <MdEditor
            v-model="form.content"
            :height="500"
            :toolbars="editorToolbars"
            :readonly="isEdit && !canEdit"
            @on-upload-img="handleUploadImage"
            :placeholder="$t('article.markdownPlaceholder')" />
        </div>
      </el-form-item>
    </el-form>

    <div class="editor-actions">
      <el-button @click="$emit('cancel')">{{ $t('common.cancel') }}</el-button>
      <template v-if="isEdit">
        <el-button v-if="canEdit" type="primary" :loading="saving" @click="handleSave(form.status)">
          {{ $t('article.update') }}
        </el-button>
        <el-button v-if="canCommit" type="warning" :loading="committing" @click="handleCommit">
          {{
            form.status === ARTICLE_STATUS.REJECTED ? $t('article.resubmit') : $t('article.submitReview')
          }}
        </el-button>
      </template>
      <template v-else>
        <el-button type="primary" :loading="saving" @click="handleSave(ARTICLE_STATUS.DRAFT)">
          {{ $t('article.draft') }}
        </el-button>
        <el-button type="success" :loading="saving" @click="handleSave(ARTICLE_STATUS.PENDING)">
          {{ $t('article.submitDirectly') }}
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import {
  createArticle,
  updateArticle,
  commitArticle,
  resubmitArticle,
  getCategoriesTree,
  getTags,
  getArticle,
  uploadFile,
  getErrorMessage,
  MdEditor,
} from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const props = defineProps({
  articleId: { type: Number, default: null },
});

const emit = defineEmits(['cancel', 'saved']);

const isEdit = computed(() => !!props.articleId);
const formRef = ref(null);
const saving = ref(false);
const committing = ref(false);
const createdId = ref(null);
const categories = ref([]);
const tags = ref([]);

const ARTICLE_STATUS = {
  DRAFT: 1,
  PENDING: 2,
  APPROVED: 3,
  REJECTED: 4,
  PUBLISHED: 5,
  RETRACTED: 6,
};

const statusTagMap = {
  [ARTICLE_STATUS.DRAFT]: 'info',
  [ARTICLE_STATUS.PENDING]: 'warning',
  [ARTICLE_STATUS.APPROVED]: 'success',
  [ARTICLE_STATUS.REJECTED]: 'danger',
  [ARTICLE_STATUS.PUBLISHED]: 'primary',
  [ARTICLE_STATUS.RETRACTED]: 'info',
};
const statusLabelMap = {
  [ARTICLE_STATUS.DRAFT]: 'articleStatus.statusDraft',
  [ARTICLE_STATUS.PENDING]: 'articleStatus.statusPending',
  [ARTICLE_STATUS.APPROVED]: 'articleStatus.statusApproved',
  [ARTICLE_STATUS.REJECTED]: 'articleStatus.statusRejected',
  [ARTICLE_STATUS.PUBLISHED]: 'articleStatus.statusPublished',
  [ARTICLE_STATUS.RETRACTED]: 'articleStatus.statusRetracted',
};
const editableStatuses = [ARTICLE_STATUS.DRAFT, ARTICLE_STATUS.REJECTED];

const statusTagType = (s) => statusTagMap[s] || 'info';
const statusLabel = (s) => statusLabelMap[s] || 'articleStatus.statusDraft';
const canEdit = computed(() => !isEdit.value || editableStatuses.includes(form.status));
const canCommit = computed(
  () => isEdit.value && (form.status === ARTICLE_STATUS.DRAFT || form.status === ARTICLE_STATUS.REJECTED),
);

const form = reactive({
  title: '',
  categoryId: null,
  termIds: [],
  summary: '',
  coverUrl: '',
  content: '',
  contentType: 'markdown',
  status: ARTICLE_STATUS.DRAFT,
});

const reviewOpinion = ref('');

const rules = computed(() => ({
  title: [{ required: true, message: t('article.title'), trigger: 'blur' }],
  categoryId: [{ required: true, message: t('article.category'), trigger: 'change' }],
  content: [{ required: true, message: t('article.content'), trigger: 'blur' }],
}));

const MEDIA_FOLDER_TYPE = 3;
const COVER_FOLDER_TYPE = 5;

const editorToolbars = [
  'bold',
  'italic',
  'underline',
  'strikeThrough',
  '-',
  'title',
  'quote',
  'unorderedList',
  'orderedList',
  '-',
  'code',
  'link',
  'image',
  '-',
  'revoke',
  'next',
  '=',
  'preview',
  'fullscreen',
  'catalog',
];

onMounted(async () => {
  try {
    const [catRes, tagRes] = await Promise.all([getCategoriesTree(), getTags({ page: 1, size: 100 })]);
    categories.value = Array.isArray(catRes) ? catRes : [];
    tags.value = tagRes.list || [];
  } catch {
    ElMessage.error(t('article.fetchFailed'));
  }

  if (isEdit.value && props.articleId) {
    try {
      const article = await getArticle(props.articleId);
      form.title = article.title || '';
      form.categoryId = article.category?.id ?? article.categoryId ?? null;
      form.termIds = article.terms?.map((t) => t.id) ?? article.termIds ?? [];
      form.summary = article.summary || '';
      form.coverUrl = article.coverUrl || '';
      form.content = article.content || '';
      form.contentType = article.contentType || 'markdown';
      form.status = Number(article.status) || 1;
      if (article.opinion) {
        reviewOpinion.value = article.opinion;
      }
    } catch {
      ElMessage.error(t('article.fetchFailed'));
    }
  }
});

const handleUploadImage = async (files, callback) => {
  try {
    const urls = [];
    for (const file of files) {
      const data = await uploadFile(file, MEDIA_FOLDER_TYPE);
      urls.push(data.url);
    }
    callback(urls);
  } catch {
    ElMessage.error(t('article.fetchFailed'));
  }
};

const handleUploadCover = async (file) => {
  try {
    const data = await uploadFile(file, COVER_FOLDER_TYPE);
    if (data.url) form.coverUrl = data.url;
  } catch {
    ElMessage.error(t('article.fetchFailed'));
  }
  return false;
};

const handleCommit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  committing.value = true;
  try {
    // Save first so edits persist even when the state-machine transition fails
    // (e.g. sensitive-word check): the article then stays a DRAFT with its content.
    await updateArticle(props.articleId, { ...form });
    // Article.submit() only accepts DRAFT and Article.resubmit() only accepts REJECTED,
    // so the endpoint has to follow the current status -- calling /commit on a rejected
    // article always fails with error.article.submit_requires_draft.
    if (form.status === ARTICLE_STATUS.REJECTED) {
      await resubmitArticle(props.articleId);
      ElMessage.success(t('article.resubmitSuccess'));
    } else {
      await commitArticle(props.articleId);
      ElMessage.success(t('article.submitReviewSuccess'));
    }
    emit('saved');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    committing.value = false;
  }
};

const handleSave = async (status) => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    // `status` is deliberately NOT part of the request body: ArticleParam declares no
    // status field (ISS-030) and Jackson drops unknown properties, while
    // ArticleConverter hardcodes DRAFT on create -- so passing it silently did nothing
    // yet still toasted "提交审核成功". Status transitions belong to the state machine
    // (/commit, /resubmit, /review, /publish, /retract), therefore "直接提交" saves
    // first and then submits the article it just created.
    if (isEdit.value) {
      await updateArticle(props.articleId, { ...form });
      ElMessage.success(t('article.updateSuccess'));
    } else {
      let id = createdId.value;
      if (id) {
        // The article was already created by an earlier "直接提交" whose commit failed
        // (e.g. sensitive-word check) -- retry through update, never create twice.
        await updateArticle(id, { ...form });
      } else {
        const created = await createArticle({ ...form });
        id = created.id;
        createdId.value = id;
      }
      if (status === ARTICLE_STATUS.PENDING) {
        await commitArticle(id);
      }
      ElMessage.success(
        status === ARTICLE_STATUS.DRAFT ? t('article.saveDraftSuccess') : t('article.submitReviewSuccess'),
      );
    }
    emit('saved');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.article-editor {
  max-width: 1200px;
  margin: 0 auto;
}
.editor-wrapper {
  width: 100%;
  margin: 20px 0;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}
.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
.cover-upload {
  display: flex;
  gap: 8px;
  width: 100%;
}
.cover-upload .el-input {
  flex: 1;
}
.review-opinion {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  padding: 8px 12px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  line-height: 1.5;
  max-width: 500px;
}
.readonly-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
