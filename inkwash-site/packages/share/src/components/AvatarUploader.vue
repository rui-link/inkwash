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
  <div class="avatar-uploader">
    <UserAvatar :src="preview" :name="name" :size="96" />
    <div class="avatar-uploader-actions">
      <el-button size="small" @click="pickFile">{{ t('avatarUpload.changeAvatar') }}</el-button>
    </div>
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      class="avatar-uploader-input"
      @change="onFileSelected" />
    <el-dialog
      v-model="cropDialogVisible"
      :title="t('avatarUpload.crop')"
      width="420px"
      :close-on-click-modal="false"
      @opened="initCropper">
      <div class="crop-container">
        <img ref="cropImg" :src="rawPreviewUrl" alt="" />
      </div>
      <template #footer>
        <el-button @click="cancelCrop">{{ t('avatarUpload.cancel') }}</el-button>
        <el-button type="primary" @click="confirmCrop">{{ t('avatarUpload.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import Cropper from 'cropperjs';
import { ElMessage } from 'element-plus';
import { ref, computed, watch, onBeforeUnmount } from 'vue';
import { useI18n } from 'vue-i18n';

import 'cropperjs/dist/cropper.css';
import UserAvatar from './UserAvatar.vue';

const props = defineProps({
  modelValue: { type: String, default: '' },
  name: { type: String, default: 'U' },
});

const emit = defineEmits(['update:modelValue', 'change']);

const { t } = useI18n();

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
const MAX_SIZE = 2 * 1024 * 1024;

const fileInput = ref(null);
const cropImg = ref(null);
const cropDialogVisible = ref(false);
const rawPreviewUrl = ref('');
const croppedPreviewUrl = ref('');
let cropper = null;
let currentFile = null;

const preview = computed(() => croppedPreviewUrl.value || props.modelValue);

watch(
  () => props.modelValue,
  (newVal) => {
    if (croppedPreviewUrl.value && newVal !== croppedPreviewUrl.value) {
      URL.revokeObjectURL(croppedPreviewUrl.value);
      croppedPreviewUrl.value = '';
    }
  },
);

function pickFile() {
  fileInput.value?.click();
}

function onFileSelected(event) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  if (!ALLOWED_TYPES.includes(file.type)) {
    ElMessage.error(t('avatarUpload.typeNotAllowed'));
    return;
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error(t('avatarUpload.sizeExceeded'));
    return;
  }
  currentFile = file;
  rawPreviewUrl.value = URL.createObjectURL(file);
  cropDialogVisible.value = true;
}

function initCropper() {
  if (!cropImg.value) return;
  destroyCropper();
  cropper = new Cropper(cropImg.value, {
    aspectRatio: 1,
    viewMode: 1,
    autoCropArea: 1,
    responsive: true,
  });
}

function confirmCrop() {
  if (!cropper || !currentFile) return;
  const canvas = cropper.getCroppedCanvas({
    width: 256,
    height: 256,
    imageSmoothingQuality: 'high',
  });
  canvas.toBlob((blob) => {
    if (!blob) return;
    if (croppedPreviewUrl.value) URL.revokeObjectURL(croppedPreviewUrl.value);
    croppedPreviewUrl.value = URL.createObjectURL(blob);
    const ext = currentFile.name.match(/\.\w+$/) ? currentFile.name.match(/\.\w+$/)[0] : '.png';
    const croppedFile = new File([blob], `avatar${ext}`, { type: blob.type });
    emit('change', croppedFile);
    closeCropDialog();
  }, 'image/png');
}

function cancelCrop() {
  closeCropDialog();
}

function closeCropDialog() {
  cropDialogVisible.value = false;
  destroyCropper();
  if (rawPreviewUrl.value) URL.revokeObjectURL(rawPreviewUrl.value);
  rawPreviewUrl.value = '';
  currentFile = null;
}

function destroyCropper() {
  if (cropper) {
    cropper.destroy();
    cropper = null;
  }
}

onBeforeUnmount(() => {
  destroyCropper();
  if (croppedPreviewUrl.value) URL.revokeObjectURL(croppedPreviewUrl.value);
});
</script>

<style scoped>
.avatar-uploader {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-uploader-input {
  display: none;
}

.crop-container {
  width: 100%;
  height: 320px;
}

.crop-container img {
  max-width: 100%;
  max-height: 100%;
  display: block;
}
</style>
