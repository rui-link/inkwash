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
  <div class="interaction-bar">
    <button
      class="interaction-btn"
      :class="{ active: states.agreed }"
      @click="handleAgree"
      :disabled="loading.agree"
      :aria-label="t('myInteractions.agreed')">
      <AppIcon name="thumbUp" :size="18" />
      <span>{{ counts.agree }}</span>
    </button>

    <button
      class="interaction-btn"
      :class="{ active: states.aversed }"
      @click="handleAverse"
      :disabled="loading.averse"
      :aria-label="t('myInteractions.aversed')">
      <AppIcon name="thumbDown" :size="18" />
      <span>{{ counts.averse }}</span>
    </button>

    <button
      class="interaction-btn"
      :class="{ active: states.favorited }"
      @click="handleFavorite"
      :disabled="loading.favorite"
      :aria-label="t('myInteractions.favorited')">
      <AppIcon name="star" :size="18" />
      <span>{{ counts.favorite }}</span>
    </button>

    <button class="interaction-btn" @click="handleShareClick" :disabled="loading.share" :aria-label="shareLabel">
      <AppIcon name="share" :size="18" />
      <span>{{ counts.share }}</span>
    </button>
  </div>
</template>

<script setup>
import {
  agreeArticle,
  unagreeArticle,
  dislikeArticle,
  undislikeArticle,
  favoriteArticle,
  unfavoriteArticle,
  shareArticle,
  getArticleInteractions,
  getErrorMessage,
  useAuthStore,
} from '@inkwash/share';
import { ElMessage } from 'element-plus';
import { reactive, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import AppIcon from '@/components/AppIcon.vue';

const props = defineProps({
  articleId: { type: [Number, String], required: true },
  tally: {
    type: Object,
    default: () => ({
      agreeCount: 0,
      averseCount: 0,
      favoriteCount: 0,
      shareCount: 0,
    }),
  },
});

const emit = defineEmits(['update']);

const { t } = useI18n();
const authStore = useAuthStore();
const router = useRouter();

const states = reactive({ agreed: false, aversed: false, favorited: false });
const loading = reactive({
  agree: false,
  averse: false,
  favorite: false,
  share: false,
});
const counts = reactive({
  agree: 0,
  averse: 0,
  favorite: 0,
  share: 0,
});

const shareLabel = computed(() => t('interactionBar.share'));

onMounted(() => {
  counts.agree = props.tally.agreeCount || 0;
  counts.averse = props.tally.averseCount || 0;
  counts.favorite = props.tally.favoriteCount || 0;
  counts.share = props.tally.shareCount || 0;
  if (authStore.isLoggedIn) fetchInteractions();
});

async function fetchInteractions() {
  try {
    const res = await getArticleInteractions(props.articleId);
    states.agreed = res.agreed;
    states.aversed = res.aversed;
    states.favorited = res.favorited;
  } catch (e) {
    console.error('Failed to fetch interactions:', e);
  }
}

function requireAuth() {
  if (!authStore.isLoggedIn) {
    router.push({ name: 'Login' });
    return false;
  }
  return true;
}

async function toggleAgree() {
  if (!requireAuth()) return;
  loading.agree = true;
  const prevAgreed = states.agreed;
  const prevAversed = states.aversed;
  const prevAgreeCount = counts.agree;
  const prevAverseCount = counts.averse;
  try {
    if (states.agreed) {
      await unagreeArticle(props.articleId);
      states.agreed = false;
      counts.agree = Math.max(0, prevAgreeCount - 1);
    } else {
      await agreeArticle(props.articleId);
      states.agreed = true;
      if (states.aversed) {
        states.aversed = false;
        counts.averse = Math.max(0, prevAverseCount - 1);
      }
      counts.agree = prevAgreeCount + 1;
    }
    emit('update', { ...states });
  } catch (e) {
    states.agreed = prevAgreed;
    states.aversed = prevAversed;
    counts.agree = prevAgreeCount;
    counts.averse = prevAverseCount;
    throw e;
  } finally {
    loading.agree = false;
  }
}

async function toggleAverse() {
  if (!requireAuth()) return;
  loading.averse = true;
  const prevAgreed = states.agreed;
  const prevAversed = states.aversed;
  const prevAgreeCount = counts.agree;
  const prevAverseCount = counts.averse;
  try {
    if (states.aversed) {
      await undislikeArticle(props.articleId);
      states.aversed = false;
      counts.averse = Math.max(0, prevAverseCount - 1);
    } else {
      await dislikeArticle(props.articleId);
      states.aversed = true;
      if (states.agreed) {
        states.agreed = false;
        counts.agree = Math.max(0, prevAgreeCount - 1);
      }
      counts.averse = prevAverseCount + 1;
    }
    emit('update', { ...states });
  } catch (e) {
    states.agreed = prevAgreed;
    states.aversed = prevAversed;
    counts.agree = prevAgreeCount;
    counts.averse = prevAverseCount;
    throw e;
  } finally {
    loading.averse = false;
  }
}

async function toggleFavorite() {
  if (!requireAuth()) return;
  loading.favorite = true;
  const prevFavorited = states.favorited;
  const prevFavoriteCount = counts.favorite;
  try {
    if (states.favorited) {
      await unfavoriteArticle(props.articleId);
      states.favorited = false;
      counts.favorite = Math.max(0, prevFavoriteCount - 1);
    } else {
      await favoriteArticle(props.articleId);
      states.favorited = true;
      counts.favorite = prevFavoriteCount + 1;
    }
    emit('update', { ...states });
  } catch (e) {
    states.favorited = prevFavorited;
    counts.favorite = prevFavoriteCount;
    throw e;
  } finally {
    loading.favorite = false;
  }
}

async function handleShare() {
  loading.share = true;
  const prevShareCount = counts.share;
  try {
    await shareArticle(props.articleId);
    counts.share = prevShareCount + 1;
    const url = window.location.origin + '/article/' + props.articleId;
    await navigator.clipboard.writeText(url);
    return { success: true, url };
  } catch (e) {
    counts.share = prevShareCount;
    throw e;
  } finally {
    loading.share = false;
  }
}

async function handleAgree() {
  try {
    await toggleAgree();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('interactionBar.operationFailed')));
  }
}

async function handleAverse() {
  try {
    await toggleAverse();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('interactionBar.operationFailed')));
  }
}

async function handleFavorite() {
  try {
    await toggleFavorite();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('interactionBar.operationFailed')));
  }
}

async function handleShareClick() {
  try {
    await handleShare();
    ElMessage.success(t('interactionBar.linkCopied'));
  } catch (e) {
    ElMessage.error(getErrorMessage(e, t('interactionBar.operationFailed')));
  }
}
</script>

<style scoped>
.interaction-bar {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.interaction-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: var(--surface-bg);
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.interaction-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 5%, transparent);
}

.interaction-btn.active {
  border-color: var(--accent-color);
  color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 10%, transparent);
}

.interaction-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.interaction-btn svg {
  flex-shrink: 0;
}
</style>
