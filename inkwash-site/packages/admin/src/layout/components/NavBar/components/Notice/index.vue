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
  <el-dropdown trigger="click" placement="bottom-end" @visible-change="handleDropdownVisible">
    <span class="dropdown-badge select-none">
      <el-badge :value="unread" :max="99">
        <span class="header-notice-icon">
          <SvgIcon icon-class="bell" />
        </span>
      </el-badge>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <div class="notice-dropdown" style="width: 500px">
          <div class="notice-header">
            <span class="notice-header-title">{{ $t('notice.title') }}</span>
            <el-button v-if="unread > 0" type="primary" link size="small" @click.stop="handleMarkAllRead">
              {{ $t('notice.markAllRead') }}
            </el-button>
          </div>
          <el-empty v-if="notices.length === 0" :description="$t('notice.empty')" :image-size="60" />
          <el-table v-else :data="notices" style="width: 100%" max-height="400">
            <el-table-column prop="title" :label="$t('notice.columnTitle')" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <span :class="{ 'notice-unread': row.readStatus === 1 }">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="type" :label="$t('notice.columnType')" width="60">
              <template #default="{ row }">
                <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="readStatus" :label="$t('notice.columnStatus')" width="60">
              <template #default="{ row }">
                <el-tag v-if="row.readStatus" size="small" :type="row.readStatus === 1 ? 'danger' : 'info'">
                  {{ row.readStatus === 1 ? $t('notice.statusUnread') : $t('notice.statusRead') }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" :label="$t('notice.columnTime')" width="180">
              <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { noticeApi, formatDate } from '@inkwash/share';
import { ref, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

import SvgIcon from '@/components/SvgIcon/index.vue';

const { t } = useI18n();

const unread = ref(0);
const notices = ref([]);
let timer = null;

async function fetchUnread() {
  try {
    const res = await noticeApi.getUnreadCount();
    unread.value = res?.count || 0;
  } catch (error) {
    console.error('Error fetching unread count:', error);
  }
}

async function fetchNotices() {
  try {
    const res = await noticeApi.getActiveNotices();
    const list = Array.isArray(res) ? res : res?.data || res?.list || [];
    notices.value = list;
  } catch (error) {
    console.error('Error fetching notices:', error);
  }
}

function handleDropdownVisible(visible) {
  if (visible) {
    fetchUnread();
    fetchNotices();
  }
}

async function handleMarkAllRead() {
  try {
    await noticeApi.markNoticesRead({ all: true });
    fetchUnread();
    fetchNotices();
  } catch (error) {
    console.error('Error marking notices read:', error);
  }
}

function typeLabel(type) {
  if (type === 1) return t('notice.typeNotification');
  if (type === 3) return t('notice.typeWarning');
  return t('notice.typeAnnouncement');
}

onMounted(() => {
  fetchUnread();
  timer = setInterval(fetchUnread, 60000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style lang="scss" scoped>
.dropdown-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 48px;
  margin-right: 10px;
  cursor: pointer;

  .header-notice-icon {
    font-size: 18px;
  }
}

.notice-dropdown {
  padding: 12px;

  .notice-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .notice-header-title {
      font-weight: 600;
    }
  }

  .notice-unread {
    font-weight: 600;
  }
}
</style>
