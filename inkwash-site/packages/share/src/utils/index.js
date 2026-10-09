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
export * from './format.js';
export * from './validate.js';
export * from './constant.js';
export * from './region.js';
export * from './error.js';
export * from './page.js';
export * from './menu.js';
export * from './avatar.js';
export * from './upload.js';
export * from './permission.js';
export * from './markdown.js';
import 'md-editor-v3/lib/style.css';
export { createI18n } from 'vue-i18n';
export { default as i18n } from '../locale/index.js';
