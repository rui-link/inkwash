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
  <div class="auth-layout">
    <header class="auth-header">
      <div class="header-inner container">
        <MenuToggle
          :open="showMobileSidebar"
          :label="$t('layout.toggleNav')"
          margin="right"
          @toggle="showMobileSidebar = !showMobileSidebar" />

        <BrandLogo src="/favicon.ico" :name="siteStore.name" :width="26" :height="26" />
        <div class="header-actions">
          <LocaleSwitcher />
          <el-dropdown @command="handleUserCommand">
            <span class="user-info">
              <UserAvatar :src="authStore.user?.avatar" :name="authStore.user?.nickname" :size="32" />
              <span class="username hide-mobile">{{ authStore.user?.nickname || 'User' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">{{ $t('layout.home') }}</el-dropdown-item>
                <el-dropdown-item command="profile">{{ $t('layout.profile') }}</el-dropdown-item>
                <el-dropdown-item divided command="logout">{{ $t('layout.signOut') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div class="mobile-sidebar-overlay" v-if="showMobileSidebar" @click="showMobileSidebar = false" />

    <div class="auth-main">
      <aside class="auth-sidebar" :class="{ 'mobile-open': showMobileSidebar }">
        <nav class="sidebar-nav">
          <router-link
            v-for="item in sidebarItems"
            :key="item.name"
            :to="{ name: item.name }"
            class="nav-item"
            :class="{ active: activeMenu.startsWith(item.path || item.name) }"
            @click="showMobileSidebar = false">
            <SvgIcon v-if="iconMap[item.name]" :icon-class="iconMap[item.name]" class="nav-icon" />
            {{ $t('layout.' + i18nKey(item.name)) }}
          </router-link>
        </nav>
      </aside>

      <main class="auth-content">
        <ErrorBoundary>
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </ErrorBoundary>
      </main>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore, useSiteStore, UserAvatar } from '@inkwash/share';
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import BrandLogo from '@/components/BrandLogo.vue';
import ErrorBoundary from '@/components/ErrorBoundary.vue';
import LocaleSwitcher from '@/components/LocaleSwitcher.vue';
import MenuToggle from '@/components/MenuToggle.vue';
import SvgIcon from '@/components/SvgIcon/index.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const siteStore = useSiteStore();

onMounted(() => {
  siteStore.fetchSiteInfo();
});

const showMobileSidebar = ref(false);

const activeMenu = computed(() => route.path);

const sidebarItems = computed(() => {
  const userRoute = router.options.routes.find((r) => r.path === '/user');
  const items = (userRoute?.children || []).filter((c) => c.name && c.meta?.sidebar !== false);
  const articleNew = items.find((c) => c.name === 'ArticleNew');
  if (articleNew) {
    return [articleNew, ...items.filter((c) => c.name !== 'ArticleNew')];
  }
  return items;
});

const iconMap = {
  ArticleNew: 'edit',
  MyArticles: 'file-text',
  MyInteractions: 'comment',
  Profile: 'user',
  SecuritySettings: 'lock',
};

function i18nKey(name) {
  const map = {
    MyArticles: 'myArticles',
    ArticleNew: 'writeArticle',
    MyInteractions: 'myInteractions',
    Profile: 'profile',
    SecuritySettings: 'accountSecurity',
  };
  return map[name] || name;
}

function handleUserCommand(command) {
  switch (command) {
    case 'home':
      router.push('/');
      break;
    case 'profile':
      router.push({ name: 'Profile' });
      break;
    case 'logout':
      authStore.logout().then(() => router.push({ name: 'ArticleList' }));
      break;
  }
}
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
}

.auth-header {
  background: var(--header-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 20;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 60px;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--bg-secondary);
}

.user-avatar {
  background: var(--accent-color) !important;
  font-family: var(--font-body);
  font-weight: 600;
}

.username {
  font-size: 14px;
  font-weight: 500;
}

.auth-main {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: clamp(16px, 3vw, 32px) clamp(16px, 3vw, 24px);
  display: flex;
  align-items: flex-start;
  gap: clamp(16px, 3vw, 32px);
  flex: 1;
  min-width: 0;
}

.auth-sidebar {
  width: 220px;
  flex-shrink: 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 8px;
  position: sticky;
  top: 92px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.nav-item:hover {
  color: var(--text-color);
  background: var(--bg-secondary);
}

.nav-item.active {
  color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 8%, transparent);
}

.nav-item .nav-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
}

.mobile-sidebar-overlay {
  display: none;
}

.auth-content {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .hide-mobile {
    display: none;
  }

  .auth-sidebar {
    display: block;
    position: fixed;
    top: 60px;
    left: 0;
    bottom: 0;
    width: 260px;
    background: var(--surface-bg);
    border-right: 1px solid var(--border-color);
    z-index: 15;
    padding: 12px;
    transform: translateX(-100%);
    transition: transform 0.3s;
    overflow-y: auto;
  }

  .auth-sidebar.mobile-open {
    transform: translateX(0);
  }

  .mobile-sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 14;
  }

  .sidebar-nav {
    border: none;
    background: transparent;
    position: static;
    padding: 0;
  }

  .auth-main {
    padding: 16px;
    gap: 0;
  }
}
</style>

<style>
.auth-content .page-enter-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}
.auth-content .page-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}
.auth-content .page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.auth-content .page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
