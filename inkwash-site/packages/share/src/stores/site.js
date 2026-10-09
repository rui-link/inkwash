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
import { ref, computed } from 'vue';

import { getSystemMeta } from '../api/system.js';

const FALLBACK_NAME = 'Inkwash';

export const useSiteStore = defineStore('site', () => {
  const meta = ref(null);
  const loaded = ref(false);
  let pending = null;

  const name = computed(() => {
    const m = meta.value;
    return m?.shortName || m?.projectName || FALLBACK_NAME;
  });

  const fullName = computed(() => {
    const m = meta.value;
    return m?.formalName || m?.shortName || m?.projectName || FALLBACK_NAME;
  });

  function applyTitle() {
    if (typeof document !== 'undefined') {
      document.title = name.value;
    }
  }

  async function fetchSiteInfo(force = false) {
    if (!force && (loaded.value || pending)) {
      return pending || Promise.resolve(meta.value);
    }
    pending = getSystemMeta()
      .then((res) => {
        meta.value = res && typeof res === 'object' ? res : {};
        loaded.value = true;
        applyTitle();
        return meta.value;
      })
      .catch(() => {
        meta.value = meta.value || {};
        loaded.value = true;
        return meta.value;
      })
      .finally(() => {
        pending = null;
      });
    return pending;
  }

  return { meta, loaded, name, fullName, fetchSiteInfo };
});
