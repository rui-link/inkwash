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

export function uploadFile(data, folderType, onProgress) {
  const formData =
    data instanceof FormData
      ? data
      : (() => {
          const fd = new FormData();
          fd.append('file', data);
          return fd;
        })();
  if (folderType != null) {
    formData.set('folderType', String(folderType));
  }
  return http.post('/support/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress,
  });
}

export function getFileInfo(id) {
  return http.get(`/support/file/${id}`);
}

export function downloadFile(id) {
  return http.get(`/support/file/${id}/download`, {
    responseType: 'arraybuffer',
  });
}

export function getFilePage(params) {
  return http.get('/support/file/page', { params });
}

export function deleteFile(id) {
  return http.delete(`/support/file/${id}`);
}
