/*
 * This file is part of Inkwash.
 * Copyright (C) 2026 ruilink team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import { defineStore } from 'pinia';
import { ref } from 'vue';

import { getLicenseInfo } from '../api/system.js';

export const useLicenseStore = defineStore('license', () => {
  const edition = ref('');
  const maxUsers = ref(null);
  const licensed = ref(true);
  const currentUserCount = ref(0);
  const tierExceeded = ref(false);
  const restrictedModules = ref([]);
  const allowedModules = ref([]);
  const holder = ref('');

  async function fetchLicenseInfo() {
    try {
      const info = await getLicenseInfo();
      if (info) {
        licensed.value = info.licensed !== false;
        edition.value = info.edition || '';
        maxUsers.value = info.maxUsers;
        currentUserCount.value = info.currentUserCount || 0;
        tierExceeded.value = info.tierExceeded === true;
        allowedModules.value = info.allowedModules || [];
        restrictedModules.value = info.allowedModules
          ? ['system', 'cms', 'monitor'].filter((m) => !info.allowedModules.includes(m))
          : [];
        holder.value = info.licenseData?.holder || '';
      }
    } catch (e) {
      console.error('Failed to fetch license info:', e);
    }
  }

  return {
    edition,
    maxUsers,
    licensed,
    currentUserCount,
    tierExceeded,
    restrictedModules,
    allowedModules,
    holder,
    fetchLicenseInfo,
  };
});
