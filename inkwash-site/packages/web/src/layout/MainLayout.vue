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
  <div class="main-layout" :class="{ 'auth-route': isAuthRoute }">
    <header class="main-header">
      <div class="header-inner container">
        <MenuToggle
          :open="mobileMenuOpen"
          :label="$t('layout.toggleNav')"
          margin="left"
          @toggle="mobileMenuOpen = !mobileMenuOpen" />

        <BrandLogo src="/favicon.ico" :name="siteStore.name" :width="28" :height="28" text-size="22px" />

        <div class="header-search" v-if="showSearch">
          <el-input
            v-model="searchKeyword"
            :placeholder="$t('layout.searchPlaceholder')"
            clearable
            @keyup.enter="handleSearch">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <nav class="header-nav">
          <router-link to="/" class="nav-link">{{ $t('layout.home') }}</router-link>
        </nav>

        <div class="header-actions">
          <LocaleSwitcher />
          <ThemeSwitcher />
          <template v-if="authStore.isLoggedIn">
            <el-tooltip :content="$t('layout.writeArticle')" placement="bottom">
              <el-button
                class="write-btn"
                :icon="EditPen"
                circle
                size="small"
                @click="router.push({ name: 'ArticleNew' })" />
            </el-tooltip>
            <el-dropdown @command="handleUserCommand">
              <span class="user-info">
                <UserAvatar :src="authStore.user?.avatar" :name="authStore.user?.nickname" :size="32" />
                <span class="username hide-mobile">{{ authStore.user?.nickname || 'User' }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="articles">{{ $t('layout.myArticles') }}</el-dropdown-item>
                  <el-dropdown-item command="interactions">{{ $t('layout.myInteractions') }}</el-dropdown-item>
                  <el-dropdown-item command="profile">{{ $t('layout.profile') }}</el-dropdown-item>
                  <el-dropdown-item divided command="logout">{{ $t('layout.signOut') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" size="small" @click="goLogin">{{ $t('layout.signIn') }}</el-button>
            <el-button size="small" @click="goRegister">{{ $t('layout.signUp') }}</el-button>
          </template>
        </div>
      </div>
    </header>

    <div class="mobile-menu-overlay" v-if="mobileMenuOpen" @click="mobileMenuOpen = false" />

    <div class="mobile-menu" :class="{ open: mobileMenuOpen }">
      <div class="mobile-menu-search" v-if="showSearch">
        <el-input
          v-model="searchKeyword"
          :placeholder="$t('layout.searchPlaceholder')"
          clearable
          @keyup.enter="handleSearchMobile">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <nav class="mobile-nav">
        <router-link to="/" class="mobile-nav-link" @click="mobileMenuOpen = false">
          {{ $t('layout.home') }}
        </router-link>
        <template v-if="authStore.isLoggedIn">
          <router-link to="/user/articles" class="mobile-nav-link" @click="mobileMenuOpen = false">
            {{ $t('layout.myArticles') }}
          </router-link>
          <router-link to="/user/interactions" class="mobile-nav-link" @click="mobileMenuOpen = false">
            {{ $t('layout.myInteractions') }}
          </router-link>
          <router-link to="/user/profile" class="mobile-nav-link" @click="mobileMenuOpen = false">
            {{ $t('layout.profile') }}
          </router-link>
        </template>
      </nav>
    </div>

    <main class="main-content" :class="{ 'auth-route': isAuthRoute }">
      <ErrorBoundary>
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </ErrorBoundary>
    </main>
    <footer class="main-footer">
      <div class="footer-inner container">
        <span class="footer-brand">{{ siteStore.fullName }}</span>
        <span class="footer-copy">{{ siteStore.meta?.copyright || $t('layout.footerCopyright') }}</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { Search, EditPen } from '@element-plus/icons-vue';
import { useAuthStore, useSiteStore, UserAvatar } from '@inkwash/share';
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

import BrandLogo from '@/components/BrandLogo.vue';
import ErrorBoundary from '@/components/ErrorBoundary.vue';
import LocaleSwitcher from '@/components/LocaleSwitcher.vue';
import MenuToggle from '@/components/MenuToggle.vue';
import ThemeSwitcher from '@/components/ThemeSwitcher.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const siteStore = useSiteStore();

onMounted(() => {
  siteStore.fetchSiteInfo();
});

const searchKeyword = ref('');
const showSearch = computed(() => !['Login', 'Register'].includes(route.name));
const isAuthRoute = computed(() => ['Login', 'Register'].includes(route.name));
const mobileMenuOpen = ref(false);

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({
      name: 'ArticleList',
      query: { search: searchKeyword.value.trim() },
    });
  }
}

function handleSearchMobile() {
  mobileMenuOpen.value = false;
  handleSearch();
}

function goLogin() {
  router.push({ name: 'Login' });
}

function goRegister() {
  router.push({ name: 'Register' });
}

function handleUserCommand(command) {
  switch (command) {
    case 'articles':
      router.push({ name: 'MyArticles' });
      break;
    case 'interactions':
      router.push({ name: 'MyInteractions' });
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
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-layout.auth-route {
  --header-bg: rgba(255, 255, 255, 0.55);

  .main-header {
    border-bottom: none;
  }

  .main-footer {
    background: rgba(255, 255, 255, 0.55);
    border-top: none;
  }

  .footer-divider {
    opacity: 0;
  }

  .main-content {
    padding: 0;
    display: flex;
    flex: 1;
  }
}

.main-header {
  background: var(--header-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  overflow: hidden;
}

.main-header::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(
      ellipse 140px 48px at 12% 100%,
      color-mix(in srgb, var(--accent-color) 16%, transparent) 0%,
      transparent 65%
    ),
    radial-gradient(
      ellipse 180px 60px at 86% 0%,
      color-mix(in srgb, var(--accent-color) 12%, transparent) 0%,
      transparent 65%
    ),
    radial-gradient(
      ellipse 260px 90px at 55% -20%,
      color-mix(in srgb, var(--primary-color) 6%, transparent) 0%,
      transparent 70%
    ),
    var(--bg-pattern);
  background-repeat: no-repeat, no-repeat, no-repeat, repeat-x;
  background-size:
    auto,
    auto,
    auto,
    150px 64px;
  opacity: 0.45;
  pointer-events: none;
}

.header-inner {
  position: relative;
  display: flex;
  align-items: center;
  height: 64px;
  gap: 24px;
}

.header-search {
  flex: 1;
  max-width: 360px;
}

.header-nav {
  display: flex;
  gap: 8px;
}

.nav-link {
  padding: 8px 16px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.2s;
}

.nav-link:hover {
  color: var(--text-color);
  background: var(--bg-secondary);
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
  color: var(--text-color);
}

.main-content {
  flex: 1;
  padding: clamp(16px, 3vw, 32px) 0;
}

.main-footer {
  background: var(--footer-bg);
  border-top: 1px solid var(--border-color);
  padding: 16px 0;
}

.footer-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
}

.footer-brand {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-color);
}

.footer-copy {
  font-size: 13px;
  color: var(--text-secondary);
}

.footer-divider {
  height: 12px;
  margin: 0;
  background-image: var(--bg-pattern);
  background-repeat: repeat-x;
  background-size: 80px 12px;
  opacity: 0.1;
}

.mobile-menu-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 90;
}

.mobile-menu {
  display: none;
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  background: var(--surface-bg);
  border-bottom: 1px solid var(--border-color);
  padding: 16px;
  z-index: 91;
  transform: translateY(-100%);
  opacity: 0;
  transition: all 0.3s;
}

.mobile-menu.open {
  transform: translateY(0);
  opacity: 1;
}

.mobile-menu-search {
  margin-bottom: 12px;
}

.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-nav-link {
  display: block;
  padding: 12px 16px;
  color: var(--text-color);
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  transition: background 0.2s;
}

.mobile-nav-link:hover {
  background: var(--bg-secondary);
}

.page-enter-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}

.page-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 768px) {
  .header-search {
    display: none;
  }
  .header-nav {
    display: none;
  }
  .hide-mobile {
    display: none;
  }
  .mobile-menu-overlay {
    display: block;
  }
  .mobile-menu {
    display: block;
  }
}
</style>
