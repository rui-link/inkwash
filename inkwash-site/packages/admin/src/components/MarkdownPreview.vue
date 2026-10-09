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
  <div class="markdown-preview" v-html="renderedContent" />
</template>

<script setup>
import { renderMarkdown } from '@inkwash/share';
import { computed } from 'vue';

const props = defineProps({
  content: { type: String, default: '' },
});

const renderedContent = computed(() => renderMarkdown(props.content));
</script>

<style scoped>
.markdown-preview {
  line-height: 1.8;
  color: var(--inkwash-text-color);
}
.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3),
.markdown-preview :deep(h4) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  font-weight: 600;
}
.markdown-preview :deep(p) {
  margin-bottom: 1em;
}
.markdown-preview :deep(code) {
  background: var(--inkwash-bg-color);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}
.markdown-preview :deep(pre code) {
  display: block;
  padding: 16px;
  overflow-x: auto;
}
.markdown-preview :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-sm);
}
.markdown-preview :deep(blockquote) {
  border-left: 3px solid var(--accent-color);
  padding-left: 16px;
  color: var(--inkwash-text-color-secondary);
  margin: 1em 0;
}
.markdown-preview :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}
.markdown-preview :deep(th),
.markdown-preview :deep(td) {
  border: 1px solid var(--inkwash-border-color);
  padding: 8px 12px;
  text-align: left;
}
.markdown-preview :deep(th) {
  background: var(--inkwash-bg-color);
  font-weight: 600;
}
</style>
