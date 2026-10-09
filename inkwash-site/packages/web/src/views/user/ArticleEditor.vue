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
    <div class="editor-header">
      <h2 class="editor-title">
        {{ isEdit ? $t('route.editArticle') : $t('route.writeArticle') }}
      </h2>
    </div>

    <el-alert
      v-if="isEdit && articleStatus === ArticleStatus.REJECTED && rejectOpinion"
      type="error"
      :title="$t('article.rejectOpinion')"
      :closable="false"
      show-icon
      class="reject-alert">
      {{ rejectOpinion }}
    </el-alert>

    <div class="editor-card">
      <el-form :model="form" ref="formRef" :rules="rules" label-width="80px" v-loading="loading">
        <el-form-item :label="$t('article.title')" prop="title">
          <el-input v-model="form.title" :placeholder="$t('article.title')" maxlength="240" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('article.summary')" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            :placeholder="$t('article.summary')"
            maxlength="500"
            show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('article.cover')" prop="coverUrl">
          <div class="cover-uploader">
            <div class="cover-preview">
              <img v-if="form.coverUrl" :src="form.coverUrl" alt="" />
              <span v-else class="cover-placeholder">{{ $t('article.uploadCover') }}</span>
            </div>
            <div class="cover-actions">
              <el-button size="small" :loading="uploadingCover" @click="coverInput?.click()">{{
                $t('article.uploadCover')
              }}</el-button>
              <el-button v-if="form.coverUrl" size="small" @click="form.coverUrl = ''">{{
                $t('common.delete')
              }}</el-button>
            </div>
            <input ref="coverInput" type="file" accept="image/*" class="hidden-input" @change="onCoverSelected" />
          </div>
        </el-form-item>
        <div class="form-row">
          <el-form-item :label="$t('article.category')" prop="categoryId">
            <el-tree-select
              v-model="form.categoryId"
              :data="categories"
              :props="{ children: 'children', label: 'name' }"
              node-key="id"
              :placeholder="$t('article.selectCategory')"
              clearable
              filterable
              style="width: 100%" />
          </el-form-item>
          <el-form-item :label="$t('article.tags')" prop="termIds">
            <el-select v-model="form.termIds" multiple :placeholder="$t('article.selectTags')" style="width: 100%">
              <el-option v-for="tag in terms" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="$t('article.content')" prop="content">
          <div class="editor-wrapper">
            <md-editor
              v-model="form.content"
              :height="500"
              :toolbars="editorToolbars"
              :placeholder="$t('article.markdownPlaceholder')"
              @on-upload-img="handleUploadImage" />
            <div class="media-actions">
              <span class="media-label">{{ $t('article.uploadMedia') }}</span>
              <el-button size="small" @click="audioInput?.click()">{{ $t('article.uploadAudio') }}</el-button>
              <el-button size="small" @click="videoInput?.click()">{{ $t('article.uploadVideo') }}</el-button>
              <input
                ref="audioInput"
                type="file"
                accept="audio/*"
                class="hidden-input"
                @change="(e) => onMediaSelected(e, 'audio')" />
              <input
                ref="videoInput"
                type="file"
                accept="video/*"
                class="hidden-input"
                @change="(e) => onMediaSelected(e, 'video')" />
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <div class="editor-actions">
            <el-button @click="handleCancel">{{ $t('common.cancel') }}</el-button>
            <el-button
              v-if="!isEdit || articleStatus === ArticleStatus.DRAFT"
              type="primary"
              :loading="saving"
              @click="handleSave()"
              >{{ $t('article.draft') }}</el-button
            >
            <el-button v-if="!isEdit" type="success" :loading="saving" @click="handleSave(ArticleStatus.PENDING)">{{
              $t('article.submitDirectly')
            }}</el-button>
            <el-button
              v-if="isEdit && (articleStatus === ArticleStatus.DRAFT || articleStatus === ArticleStatus.REJECTED)"
              type="success"
              :loading="submitting"
              @click="handleSubmit"
              >{{
                articleStatus === ArticleStatus.REJECTED ? $t('article.resubmit') : $t('article.submitReview')
              }}</el-button
            >
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import {
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
} from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref, reactive, onMounted, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const formRef = ref(null);
const loading = ref(false);
const saving = ref(false);
const submitting = ref(false);
const createdId = ref(null);
const categories = ref([]);
const terms = ref([]);
const articleStatus = ref('');
const rejectOpinion = ref('');
const coverInput = ref(null);
const audioInput = ref(null);
const videoInput = ref(null);
const uploadingCover = ref(false);

const isEdit = computed(() => !!route.params.id);

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

const form = reactive({
  title: '',
  summary: '',
  coverUrl: '',
  categoryId: null,
  termIds: [],
  content: '',
  contentType: 'markdown',
});

const rules = {
  title: [{ required: true, message: t('article.title'), trigger: 'blur' }],
  content: [{ required: true, message: t('article.content'), trigger: 'blur' }],
};

onMounted(async () => {
  try {
    const [catRes, termRes] = await Promise.all([getCategoriesTree(), getPublicTerms()]);
    categories.value = Array.isArray(catRes) ? catRes : [];
    terms.value = termRes.list || [];
  } catch {
    /* ignore */
  }
  if (isEdit.value) {
    loading.value = true;
    try {
      const article = await getPublicArticle(route.params.id);
      form.title = article.title || '';
      form.summary = article.summary || '';
      form.coverUrl = article.coverUrl || '';
      form.categoryId = article.category?.id || null;
      form.termIds = article.terms?.map((t) => t.id) || [];
      form.content = article.content || '';
      articleStatus.value = article.status || '';
      rejectOpinion.value = article.opinion || '';
    } catch {
      ElMessage.error(t('article.fetchFailed'));
      router.push({ name: 'MyArticles' });
    } finally {
      loading.value = false;
    }
  }
});

async function handleSave(status = ArticleStatus.DRAFT) {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (isEdit.value) {
      await updateUserArticle(route.params.id, { ...form });
      ElMessage.success(t('article.updateSuccess'));
    } else {
      let id = createdId.value;
      if (id) {
        // The article was already created by an earlier "直接提交" whose commit failed the
        // check (e.g. sensitive words) -- it stays a DRAFT, so retry through update and
        // never create a duplicate.
        await updateUserArticle(id, { ...form });
      } else {
        const created = await createUserArticle({ ...form });
        id = created?.id ?? null;
        createdId.value = id;
      }
      // Status transitions belong to the state machine (/commit): "直接提交" saves the
      // article first and then submits it, so a failed check leaves a DRAFT behind.
      if (status === ArticleStatus.PENDING) {
        await commitArticle(id);
      }
      ElMessage.success(
        status === ArticleStatus.DRAFT ? t('article.saveDraftSuccess') : t('article.submitReviewSuccess'),
      );
    }
    router.push({ name: 'MyArticles' });
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    saving.value = false;
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    await updateUserArticle(route.params.id, { ...form });
    // Article.submit() only accepts DRAFT and Article.resubmit() only accepts REJECTED
    if (articleStatus.value === ArticleStatus.REJECTED) {
      await resubmitArticle(route.params.id);
      ElMessage.success(t('article.resubmitSuccess'));
    } else {
      await commitArticle(route.params.id);
      ElMessage.success(t('article.committed'));
    }
    router.push({ name: 'MyArticles' });
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.operationFailed')));
  } finally {
    submitting.value = false;
  }
}

function handleCancel() {
  router.push({ name: 'MyArticles' });
}

async function onCoverSelected(event) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  uploadingCover.value = true;
  try {
    const res = await uploadCover(file);
    form.coverUrl = res.url;
    ElMessage.success(t('common.uploadSuccess'));
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.uploadFailed')));
  } finally {
    uploadingCover.value = false;
  }
}

async function handleUploadImage(files, callback) {
  try {
    const urls = [];
    for (const file of files) {
      const res = await uploadMedia(file);
      urls.push(res.url);
    }
    callback(urls);
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.uploadFailed')));
  }
}

async function onMediaSelected(event, type) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  try {
    const res = await uploadMedia(file);
    const escapedUrl = res.url
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
    const tag =
      type === 'audio'
        ? `\n<audio controls src="${escapedUrl}"></audio>\n`
        : `\n<video controls src="${escapedUrl}"></video>\n`;
    form.content = (form.content || '') + tag;
    ElMessage.success(t('common.uploadSuccess'));
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('common.uploadFailed')));
  }
}
</script>

<style scoped>
.editor-header {
  margin-bottom: 24px;
}

.editor-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
}

.reject-alert {
  margin-bottom: 20px;
}

.editor-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 28px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}

.cover-uploader {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  flex-wrap: wrap;
}

.cover-preview {
  width: 160px;
  height: 90px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-subtle);
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  font-size: 12px;
  color: var(--text-secondary);
}

.cover-actions {
  display: flex;
  gap: 8px;
}

.hidden-input {
  display: none;
}

.editor-wrapper {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.media-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-subtle);
  flex-wrap: wrap;
}

.media-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-right: 4px;
}

.editor-actions {
  display: flex;
  gap: 10px;
  margin-left: auto;
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .editor-card {
    padding: 16px;
  }

  .editor-actions {
    margin-left: 0;
    width: 100%;
  }

  .editor-actions .el-button {
    flex: 1;
  }
}
</style>
