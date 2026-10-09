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

export function getUserList(params) {
  return http.get('/system/users', { params });
}

export function getUserDetail(id) {
  return http.get(`/system/users/${id}`);
}

export function createUser(data) {
  return http.post('/system/users', data);
}

export function updateUser(id, data) {
  return http.put(`/system/users/${id}`, data);
}

export function deleteUser(id) {
  return http.delete(`/system/users/${id}`);
}

export function assignGroups(data) {
  return http.post('/system/users/groups', data);
}

export function resetUserPassword(id, data) {
  return http.post(`/system/users/${id}/reset-password`, data);
}

export function updateUserStatus(id, data) {
  return http.put(`/system/users/${id}/status`, data);
}

export function getGenderOptions() {
  return http.get('/system/users/gender');
}

export function getEducationOptions() {
  return http.get('/system/users/education');
}

export const userApi = {
  getUserList,
  getUserDetail,
  createUser,
  updateUser,
  deleteUser,
  assignGroups,
  resetUserPassword,
  updateUserStatus,
  getGenderOptions,
  getEducationOptions,
};
