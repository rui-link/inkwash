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
  <div class="comment-form">
    <el-input
      v-model="content"
      type="textarea"
      :rows="3"
      :placeholder="replyTo ? $t('commentForm.replyTo', { nickname: replyTo.nickname }) : $t('commentForm.placeholder')"
      maxlength="1000"
      show-word-limit
      :class="{ 'has-error': errorMsg }" />
    <p v-if="errorMsg" class="comment-error">{{ errorMsg }}</p>
    <div class="form-actions">
      <el-button v-if="replyTo" text @click="onCancel">{{ $t('commentForm.cancelReply') }}</el-button>
      <el-button type="primary" @click="submit" :loading="submitting">
        {{ $t('commentForm.submit') }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

const props = defineProps({
  replyTo: { type: Object, default: null },
});
const emit = defineEmits(['submit', 'cancelReply']);

const { t } = useI18n();
const content = ref('');
const submitting = ref(false);
const errorMsg = ref('');

function submit() {
  const trimmed = content.value.trim();
  if (!trimmed || submitting.value) return;
  if (trimmed.length > 1000) {
    errorMsg.value = t('commentForm.tooLong');
    return;
  }
  errorMsg.value = '';
  submitting.value = true;
  emit('submit', {
    content: trimmed,
    parentId: props.replyTo?.id,
    done: (err) => {
      submitting.value = false;
      if (err) {
        errorMsg.value = err;
      } else {
        content.value = '';
      }
    },
  });
}

function onCancel() {
  errorMsg.value = '';
  emit('cancelReply');
}
</script>

<style scoped>
.comment-form {
  margin-top: 16px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.comment-error {
  margin: 6px 2px 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--color-danger, #f56c6c);
}
.comment-form :deep(.has-error .el-textarea__inner) {
  border-color: var(--color-danger, #f56c6c);
}
</style>
