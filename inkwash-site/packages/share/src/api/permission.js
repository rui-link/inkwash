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

export function getPermissionList(params) {
  return http.get('/system/permissions', { params });
}

export function getPermissionDetail(id) {
  return http.get(`/system/permissions/${id}`);
}

export function createPermission(data) {
  return http.post('/system/permissions', data);
}

export function updatePermission(id, data) {
  return http.put(`/system/permissions/${id}`, data);
}

export function deletePermission(id) {
  return http.delete(`/system/permissions/${id}`);
}

export const permissionApi = {
  getPermissionList,
  getPermissionDetail,
  createPermission,
  updatePermission,
  deletePermission,
};
