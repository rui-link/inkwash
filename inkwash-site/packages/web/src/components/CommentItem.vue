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
  <div class="comment-item">
    <div class="comment-header">
      <el-avatar :size="24">{{ comment.nickname?.[0] }}</el-avatar>
      <span class="comment-author">{{ comment.nickname }}</span>
      <span class="comment-date">{{ formatDate(comment.createTime) }}</span>
    </div>
    <div class="comment-content">{{ comment.content }}</div>
    <div class="comment-actions" v-if="authStore.isLoggedIn">
      <el-button text size="small" @click="$emit('reply', comment)">{{ $t('commentItem.reply') }}</el-button>
    </div>
    <div class="comment-children" v-if="comment.children?.length">
      <CommentItem v-for="child in comment.children" :key="child.id" :comment="child" @reply="$emit('reply', $event)" />
    </div>
  </div>
</template>

<script setup>
import { useAuthStore, formatDate } from '@inkwash/share';

defineProps({ comment: { type: Object, required: true } });
defineEmits(['reply']);
const authStore = useAuthStore();
</script>

<style scoped>
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.comment-author {
  font-size: 14px;
  font-weight: 500;
}
.comment-date {
  font-size: 12px;
  color: var(--text-secondary);
}
.comment-content {
  font-size: 14px;
  line-height: 1.6;
  margin-left: 32px;
}
.comment-actions {
  margin-left: 32px;
  margin-top: 4px;
}
.comment-children {
  margin-left: 32px;
  margin-top: 8px;
}
</style>
