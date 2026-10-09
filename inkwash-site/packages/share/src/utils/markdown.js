import DOMPurify from 'dompurify';
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
import MarkdownIt from 'markdown-it';

export const md = new MarkdownIt({ breaks: true, html: true });

const CACHE_LIMIT = 30;
const cache = new Map();

export function renderMarkdown(content) {
  if (!content) return '';
  if (cache.has(content)) {
    const html = cache.get(content);
    cache.delete(content);
    cache.set(content, html);
    return html;
  }
  const html = DOMPurify.sanitize(md.render(content));
  if (cache.size >= CACHE_LIMIT) {
    cache.delete(cache.keys().next().value);
  }
  cache.set(content, html);
  return html;
}
