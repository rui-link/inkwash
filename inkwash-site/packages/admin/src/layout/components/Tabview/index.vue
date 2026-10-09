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
  <div class="tags-container" ref="tagsWrapper">
    <el-scrollbar class="scroll-container" :vertical="false" @wheel.prevent="handleScroll">
      <router-link
        ref="tagRef"
        v-for="tag in visitedViews"
        :key="tag.fullPath"
        :class="'tags-item ' + (isActive(tag) ? 'active' : '')"
        :to="{ path: tag.path, query: tag.query }"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : ''"
        @contextmenu.prevent="openContentMenu(tag, $event)">
        {{ translateRouteTitle(tag.title) }}
        <SvgIcon
          icon-class="close"
          class="close-icon"
          size="12px"
          v-if="!isAffix(tag)"
          @click.prevent.stop="closeSelectedTag(tag)" />
      </router-link>
    </el-scrollbar>

    <ul v-show="contentMenuVisible" class="contextmenu" :style="{ left: left + 'px', top: top + 'px' }">
      <li @click="refreshSelectedTag(selectedTag)">
        <SvgIcon icon-class="refresh" />
        {{ $t('tabview.refresh') }}
      </li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">
        <SvgIcon icon-class="close" />
        {{ $t('tabview.close') }}
      </li>
      <li @click="closeOtherTags">
        <SvgIcon icon-class="close_other" />
        {{ $t('tabview.closeOther') }}
      </li>
      <li v-if="!isFirstView()" @click="closeLeftTags">
        <SvgIcon icon-class="close_left" />
        {{ $t('tabview.closeLeft') }}
      </li>
      <li v-if="!isLastView()" @click="closeRightTags">
        <SvgIcon icon-class="close_right" />
        {{ $t('tabview.closeRight') }}
      </li>
      <li @click="closeAllTags(selectedTag)">
        <SvgIcon icon-class="close_all" />
        {{ $t('tabview.closeAll') }}
      </li>
    </ul>
  </div>
</template>

<script setup>
import { findOutermostParent } from '@inkwash/share';
import { useAppStore } from '@inkwash/share';
import { storeToRefs } from 'pinia';
import { computed, ref, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import SvgIcon from '@/components/SvgIcon/index.vue';
import { usePermissionStore, useTabviewStore, useSettingsStore } from '@/stores';
import { translateRouteTitle } from '@/utils/i18n';

const router = useRouter();
const route = useRoute();

const permissionStore = usePermissionStore();
const tabviewStore = useTabviewStore();
const appStore = useAppStore();

const { visitedViews } = storeToRefs(tabviewStore);
const settingsStore = useSettingsStore();

const selectedTag = ref({
  path: '',
  fullPath: '',
  name: '',
  title: '',
  affix: false,
  keepAlive: false,
});
const affixTags = ref([]);
const tagsWrapper = ref(null);
const left = ref(0);
const top = ref(0);

watch(
  route,
  () => {
    addTags();
    moveToCurrentTag();
  },
  { immediate: true },
);

const contentMenuVisible = ref(false);
watch(contentMenuVisible, (value) => {
  if (value) {
    document.body.addEventListener('click', closeContentMenu);
  } else {
    document.body.removeEventListener('click', closeContentMenu);
  }
});

function filterAffixTags(routes, basePath = '/') {
  let tags = [];
  routes.forEach((route) => {
    const tagPath = basePath === '/' ? `/${route.path}` : `${basePath}/${route.path}`;
    if (route.meta?.affix) {
      tags.push({
        path: tagPath,
        fullPath: tagPath,
        name: String(route.name),
        title: route.meta?.title || 'no-name',
        affix: route.meta?.affix,
        keepAlive: route.meta?.keepAlive,
      });
    }
    if (route.children) {
      const tempTags = filterAffixTags(route.children, tagPath);
      if (tempTags.length >= 1) tags = [...tags, ...tempTags];
    }
  });
  return tags;
}

function initTags() {
  const tags = filterAffixTags(permissionStore.routes);
  affixTags.value = tags;
  for (const tag of tags) {
    if (tag.name) tabviewStore.addVisitedView(tag);
  }
}

function addTags() {
  if (route.meta.title && !route.meta.noTab) {
    tabviewStore.addView({
      name: route.name,
      title: route.meta.title,
      path: route.path,
      fullPath: route.fullPath,
      affix: route.meta?.affix,
      keepAlive: route.meta?.keepAlive,
    });
  }
}

function moveToCurrentTag() {
  nextTick(() => {
    for (const tag of visitedViews.value) {
      if (tag.path === route.path) {
        if (tag.fullPath !== route.fullPath) {
          tabviewStore.updateVisitedView({
            name: route.name,
            title: route.meta.title || '',
            path: route.path,
            fullPath: route.fullPath,
            affix: route.meta?.affix,
            keepAlive: route.meta?.keepAlive,
          });
        }
      }
    }
  });
}

function isActive(tag) {
  return tag.path === route.path;
}
function isAffix(tag) {
  return tag?.affix;
}

function isFirstView() {
  try {
    return selectedTag.value.path === '/home' || selectedTag.value.fullPath === tabviewStore.visitedViews[1].fullPath;
  } catch (err) {
    return false;
  }
}

function isLastView() {
  try {
    return selectedTag.value.fullPath === tabviewStore.visitedViews[tabviewStore.visitedViews.length - 1].fullPath;
  } catch (err) {
    return false;
  }
}

function refreshSelectedTag(view) {
  tabviewStore.delCachedView(view);
  const { fullPath } = view;
  nextTick(() => {
    router.replace({ path: '/redirect' + fullPath });
  });
}

function toLastView(visitedViews, view) {
  const latestView = visitedViews.slice(-1)[0];
  if (latestView && latestView.fullPath) {
    router.push(latestView.fullPath);
  } else {
    if (view?.name === 'Home') {
      router.replace({ path: '/redirect' + view.fullPath });
    } else {
      router.push('/');
    }
  }
}

function closeSelectedTag(view) {
  const res = tabviewStore.delView(view);
  if (isActive(view)) toLastView(res.visitedViews, view);
}

function closeLeftTags() {
  const res = tabviewStore.delLeftViews(selectedTag.value);
  if (!res.visitedViews.find((item) => item.path === route.path)) toLastView(res.visitedViews);
}

function closeRightTags() {
  const res = tabviewStore.delRightViews(selectedTag.value);
  if (!res.visitedViews.find((item) => item.path === route.path)) toLastView(res.visitedViews);
}

function closeOtherTags() {
  router.push(selectedTag.value);
  tabviewStore.delOtherViews(selectedTag.value);
  moveToCurrentTag();
}

function closeAllTags(view) {
  const res = tabviewStore.delAllViews();
  toLastView(res.visitedViews, view);
}

function openContentMenu(tag, e) {
  const wrapper = tagsWrapper.value;
  const menuMinWidth = 105;
  const offsetLeft = wrapper?.getBoundingClientRect().left ?? 0;
  const offsetWidth = wrapper?.offsetWidth ?? 0;
  const maxLeft = offsetWidth - menuMinWidth;
  const l = e.clientX - offsetLeft + 15;

  left.value = l > maxLeft ? maxLeft : l;
  top.value = e.clientY;

  contentMenuVisible.value = true;
  selectedTag.value = tag;
}

function closeContentMenu() {
  contentMenuVisible.value = false;
}
function handleScroll() {
  closeContentMenu();
}

onMounted(() => {
  initTags();
});

onUnmounted(() => {
  document.body.removeEventListener('click', closeContentMenu);
});
</script>

<style lang="scss" scoped>
.tags-container {
  width: 100%;
  height: 34px;
  background-color: var(--navbar-bg);
  border: 1px solid var(--navbar-border);
  box-shadow: 0 1px 1px var(--el-box-shadow-light);

  .tags-item {
    display: inline-block;
    padding: 3px 8px;
    margin: 4px 0 0 5px;
    font-size: 12px;
    cursor: pointer;
    border: 1px solid var(--el-border-color-light);

    &:hover {
      color: var(--el-color-primary);
    }

    &:first-of-type {
      margin-left: 15px;
    }

    &:last-of-type {
      margin-right: 15px;
    }

    .close-icon {
      border-radius: 50%;
      &:hover {
        color: #fff;
        background-color: var(--el-color-primary);
      }
    }

    &.active {
      color: #fff;
      background-color: var(--el-color-primary);

      &::before {
        display: inline-block;
        width: 8px;
        height: 8px;
        margin-right: 5px;
        content: '';
        background: #fff;
        border-radius: 50%;
      }

      .close-icon:hover {
        color: var(--el-color-primary);
        background-color: var(--el-fill-color-light);
      }
    }
  }
}

.contextmenu {
  position: absolute;
  z-index: 99;
  font-size: 12px;
  background: var(--el-bg-color-overlay);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);

  li {
    padding: 8px 16px;
    cursor: pointer;

    &:hover {
      background: var(--el-fill-color-light);
    }
  }
}

.scroll-container {
  position: relative;
  width: 100%;
  overflow: hidden;
  white-space: nowrap;

  .el-scrollbar__bar {
    bottom: 0;
  }

  .el-scrollbar__wrap {
    height: 49px;
  }
}
</style>
