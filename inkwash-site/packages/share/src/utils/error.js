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
export function getErrorMessage(err, fallback = '') {
  const data = err?.response?.data;
  if (data != null) {
    if (typeof data === 'string' && data) return data;
    if (data.detail) return data.detail;
    if (data.message) return data.message;
    if (data.msg) return data.msg;
    if (data.warning) return data.warning;
  }
  return err?.message || fallback;
}

export function isLicenseError(data) {
  return !!data && (data.code === 40003 || data.requireLicense === true);
}
