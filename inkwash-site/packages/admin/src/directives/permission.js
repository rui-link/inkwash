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
import { usePermission } from '@inkwash/share';

function checkPerm(el, value) {
  if (value && value instanceof Array && value.length > 0) {
    const { hasPerm } = usePermission();
    toggle(el, hasPerm(value));
  } else {
    toggle(el, true);
  }
}

function checkRole(el, value) {
  if (value && value instanceof Array && value.length > 0) {
    const { hasRole } = usePermission();
    toggle(el, hasRole(value));
  } else {
    toggle(el, true);
  }
}

function toggle(el, allowed) {
  if (allowed) {
    el.style.display = el.__inkwashDisplay ?? '';
    el.removeAttribute('hidden');
  } else {
    if (el.style.display !== 'none') {
      el.__inkwashDisplay = el.style.display;
    }
    el.style.display = 'none';
    el.setAttribute('hidden', '');
  }
}

export const hasPerm = {
  mounted(el, binding) {
    checkPerm(el, binding.value);
  },
  updated(el, binding) {
    if (binding.value !== binding.oldValue) {
      checkPerm(el, binding.value);
    }
  },
};

export const hasRole = {
  mounted(el, binding) {
    checkRole(el, binding.value);
  },
  updated(el, binding) {
    if (binding.value !== binding.oldValue) {
      checkRole(el, binding.value);
    }
  },
};
