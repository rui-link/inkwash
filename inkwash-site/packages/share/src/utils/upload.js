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
import { uploadFile } from '../api/file.js';

export const COVER_FOLDER_TYPE = 5;
export const MEDIA_FOLDER_TYPE = 3;

export function uploadCover(file) {
  return uploadFile(file, COVER_FOLDER_TYPE);
}

export function uploadMedia(file) {
  return uploadFile(file, MEDIA_FOLDER_TYPE);
}
