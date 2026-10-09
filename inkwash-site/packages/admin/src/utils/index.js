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
export function isExternal(path) {
  return /^(https?:|http?:|mailto:|tel:)/.test(path);
}

export function handlePath(routePath, basePath) {
  if (isExternal(routePath)) return routePath;
  if (isExternal(basePath)) return basePath;
  if (!routePath) return basePath;
  const base = basePath.replace(/\/$/, '');
  return routePath.startsWith('/') ? routePath : `${base}/${routePath}`;
}

export function setStyleProperty(propName, value) {
  document.documentElement.style.setProperty(propName, value);
}
