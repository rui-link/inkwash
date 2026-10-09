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

export function getNoticeList(params) {
  return http.get('/system/notices', { params });
}

export function getActiveNotices() {
  return http.get('/system/notices/active');
}

export function getNoticeDetail(id) {
  return http.get(`/system/notices/${id}`);
}

export function createNotice(data) {
  return http.post('/system/notices', data);
}

export function updateNotice(id, data) {
  return http.put(`/system/notices/${id}`, data);
}

export function deleteNotice(id) {
  return http.delete(`/system/notices/${id}`);
}

export function batchDeleteNotices(data) {
  return http.delete('/system/notices/batch', { data });
}

export function getUnreadCount() {
  return http.get('/system/notices/unread-count');
}

export function markNoticesRead(data) {
  return http.post('/system/notices/read', data);
}

export function deleteNotices(data) {
  return http.post('/system/notices/delete', data);
}

export function clearNotices() {
  return http.post('/system/notices/clear');
}

export const noticeApi = {
  getNoticeList,
  getActiveNotices,
  getNoticeDetail,
  createNotice,
  updateNotice,
  deleteNotice,
  batchDeleteNotices,
  getUnreadCount,
  markNoticesRead,
  deleteNotices,
  clearNotices,
};
