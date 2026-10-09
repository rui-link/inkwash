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
  <div class="file-uploader">
    <div v-if="fileList.length > 0" class="file-list">
      <div v-for="(file, index) in fileList" :key="file.id || index" class="file-item">
        <div class="file-info">
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">{{ formatSize(file.size) }}</span>
        </div>
        <div class="file-actions">
          <el-button v-if="file.url" text size="small" @click="handlePreview(file.url)"> Preview </el-button>
          <el-button text size="small" type="danger" @click="handleRemove(index)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="(!multiple && fileList.length === 0) || (multiple && fileList.length < maxCount)" class="upload-area">
      <el-upload
        :show-file-list="false"
        :accept="accept"
        :multiple="multiple"
        :auto-upload="false"
        :before-upload="handleUpload">
        <el-button type="primary" :loading="uploading">
          <el-icon class="el-icon--left"><Upload /></el-icon>
          {{ uploading ? t('fileUploader.uploading', { pct: uploadProgress }) : t('fileUploader.selectFile') }}
        </el-button>
      </el-upload>
    </div>
  </div>
</template>

<script setup>
import { Plus, Upload, Delete } from '@element-plus/icons-vue';
import { uploadFile, deleteFile, getErrorMessage } from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

function isSafeUrl(url) {
  try {
    const parsed = new URL(url, window.location.origin);
    return parsed.protocol === 'http:' || parsed.protocol === 'https:';
  } catch {
    return false;
  }
}

function handlePreview(url) {
  if (!isSafeUrl(url)) {
    ElMessage.warning(t('fileUploader.invalidUrl'));
    return;
  }
  window.open(url, '_blank');
}

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  accept: { type: String, default: '*' },
  multiple: { type: Boolean, default: false },
  maxSize: { type: Number, default: 10 },
  maxCount: { type: Number, default: 5 },
  listType: { type: String, default: 'text' },
  folderType: { type: Number, default: 1 },
});

const emit = defineEmits(['update:modelValue']);

const { t } = useI18n();
const uploading = ref(false);
const uploadProgress = ref(0);
const fileList = ref(props.modelValue || []);

async function handleUpload(file) {
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(t('fileUploader.sizeExceeded', { max: props.maxSize }));
    return false;
  }

  if (!props.multiple && fileList.value.length >= 1) {
    ElMessage.warning(t('fileUploader.onlyOne'));
    return false;
  }

  if (props.multiple && fileList.value.length >= props.maxCount) {
    ElMessage.warning(t('fileUploader.maxCount', { max: props.maxCount }));
    return false;
  }

  uploading.value = true;
  uploadProgress.value = 0;
  try {
    const data = await uploadFile(file, props.folderType, (e) => {
      if (e.total) uploadProgress.value = Math.round((e.loaded / e.total) * 100);
    });
    const fileInfo = {
      id: data.id,
      name: data.name || file.name,
      url: data.url,
      size: file.size,
      type: file.type,
    };
    const updated = props.multiple ? [...fileList.value, fileInfo] : [fileInfo];
    fileList.value = updated;
    emit('update:modelValue', updated);
    ElMessage.success('Uploaded successfully');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, 'Upload failed'));
  } finally {
    uploading.value = false;
    uploadProgress.value = 0;
  }
}

async function handleRemove(index) {
  const file = fileList.value[index];
  try {
    if (file.id) await deleteFile(file.id);
  } catch (e) {
    ElMessage.error(getErrorMessage(e, 'Delete failed'));
  }
  const updated = fileList.value.filter((_, i) => i !== index);
  fileList.value = updated;
  emit('update:modelValue', updated);
}

function formatSize(bytes) {
  if (!bytes) return '';
  const mb = bytes / (1024 * 1024);
  return mb >= 1 ? `${mb.toFixed(1)} MB` : `${(bytes / 1024).toFixed(0)} KB`;
}
</script>

<style scoped>
.file-list {
  margin-bottom: 12px;
}
.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  margin-bottom: 6px;
  background: var(--el-fill-color-lighter);
}
.file-info {
  display: flex;
  gap: 12px;
  align-items: center;
}
.file-name {
  font-size: 13px;
  font-weight: 500;
}
.file-size {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.file-actions {
  display: flex;
  gap: 4px;
}
</style>
