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

export function getRoleList(params) {
  return http.get('/system/roles', { params });
}

export function getRoleDetail(id) {
  return http.get(`/system/roles/${id}`);
}

export function createRole(data) {
  return http.post('/system/roles', data);
}

export function updateRole(id, data) {
  return http.put(`/system/roles/${id}`, data);
}

export function deleteRole(id) {
  return http.delete(`/system/roles/${id}`);
}

export function assignPermissions(data) {
  return http.post('/system/roles/permissions', data);
}

export const roleApi = {
  getRoleList,
  getRoleDetail,
  createRole,
  updateRole,
  deleteRole,
  assignPermissions,
};
