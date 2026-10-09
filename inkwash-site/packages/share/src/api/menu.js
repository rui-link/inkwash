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

export function getMenuList(params) {
  return http.get('/system/menus', { params });
}

export function getMenuTree() {
  return http.get('/system/menus/tree');
}

export function getMenuDetail(id) {
  return http.get(`/system/menus/${id}`);
}

export function getUserMenus() {
  return http.get('/system/menus/user');
}

export function createMenu(data) {
  return http.post('/system/menus', data);
}

export function updateMenu(id, data) {
  return http.put(`/system/menus/${id}`, data);
}

export function deleteMenu(id) {
  return http.delete(`/system/menus/${id}`);
}

export const menuApi = {
  getMenuList,
  getMenuTree,
  getMenuDetail,
  getUserMenus,
  createMenu,
  updateMenu,
  deleteMenu,
};
