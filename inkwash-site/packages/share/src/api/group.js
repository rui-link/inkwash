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

export function getGroupList(params) {
  return http.get('/system/groups', { params });
}

export function getGroupTree() {
  return http.get('/system/groups/tree');
}

export function getGroupDetail(id) {
  return http.get(`/system/groups/${id}`);
}

export function createGroup(data) {
  return http.post('/system/groups', data);
}

export function updateGroup(id, data) {
  return http.put(`/system/groups/${id}`, data);
}

export function deleteGroup(id) {
  return http.delete(`/system/groups/${id}`);
}

export function assignRoles(data) {
  return http.post('/system/groups/roles', data);
}

export const groupApi = {
  getGroupList,
  getGroupTree,
  getGroupDetail,
  createGroup,
  updateGroup,
  deleteGroup,
  assignRoles,
};
