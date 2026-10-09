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

export function getSensitiveList(params) {
  return http.get('/cms/sensitive', { params });
}

export function getSensitiveDetail(id) {
  return http.get(`/cms/sensitive/${id}`);
}

export function createSensitive(data) {
  return http.post('/cms/sensitive', data);
}

export function updateSensitive(id, data) {
  return http.put(`/cms/sensitive/${id}`, data);
}

export function deleteSensitive(id) {
  return http.delete(`/cms/sensitive/${id}`);
}

export const sensitiveApi = {
  getSensitiveList,
  getSensitiveDetail,
  createSensitive,
  updateSensitive,
  deleteSensitive,
};
