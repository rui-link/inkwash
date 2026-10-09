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
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="flex-x-between">
          <span class="text-lg font-bold">{{ isEdit ? $t('article.editArticle') : $t('article.newArticle') }}</span>
        </div>
      </template>
      <ArticleEditor :article-id="articleId" @cancel="handleCancel" @saved="handleSaved" />
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import ArticleEditor from '@/components/ArticleEditor.vue';

const route = useRoute();
const router = useRouter();

const articleId = computed(() => route.query.id || null);
const isEdit = computed(() => !!articleId.value);

function handleCancel() {
  router.push('/cms/article');
}

function handleSaved() {
  router.push('/cms/article');
}
</script>
