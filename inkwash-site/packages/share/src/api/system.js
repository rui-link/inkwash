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
import http from '../http/index.js';

export function getSystemMeta() {
  return http.get('/system/meta');
}

export function getAboutInfo() {
  return http.get('/system/about');
}

export function getCmsDashboard() {
  return http.get('/cms/dashboard');
}

export function getArticleStats(range) {
  return http.get('/cms/dashboard/article-stats', { params: { range } });
}

export function getUserStats(range) {
  return http.get('/cms/dashboard/user-stats', { params: { range } });
}

export function getCategoryStats(range) {
  return http.get('/cms/dashboard/category-stats', { params: { range } });
}

export function getMyArticleStats(range) {
  return http.get('/cms/dashboard/my-article-stats', { params: { range } });
}

export function getDashboardSummary() {
  return http.get('/cms/dashboard/summary');
}

export function getLicenseInfo() {
  return http.get('/license/info');
}
