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
import { ArticleStatusMap, ArticleStatusLabelKey } from '@inkwash/share';

export function truncate(text, len = 100) {
  if (!text) return '';
  return text.length > len ? text.slice(0, len) + '...' : text;
}

export function statusKey(status) {
  return ArticleStatusLabelKey[Number(status)] || 'statusDraft';
}

export function statusLabel(status) {
  return `articleStatus.${statusKey(status)}`;
}

export function statusType(status) {
  return ArticleStatusMap[Number(status)]?.type || 'info';
}
