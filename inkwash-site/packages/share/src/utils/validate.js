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
export function isEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function isPhone(value) {
  return /^1[3-9]\d{9}$/.test(value);
}

export function isUrl(value) {
  return /^https?:\/\//.test(value);
}

export function isExternal(value) {
  return isUrl(value) || isEmail(value) || isPhone(value);
}

export const rules = {
  required: (msg = 'This field is required') => ({
    required: true,
    message: msg,
    trigger: 'blur',
  }),
  email: (msg = 'Invalid email') => ({
    validator: (r, v, cb) => (!v || isEmail(v) ? cb() : cb(new Error(msg))),
    trigger: 'blur',
  }),
  phone: (msg = 'Invalid phone') => ({
    validator: (r, v, cb) => (!v || isPhone(v) ? cb() : cb(new Error(msg))),
    trigger: 'blur',
  }),
  min: (min, msg) => ({ min, message: msg, trigger: 'blur' }),
  max: (max, msg) => ({ max, message: msg, trigger: 'blur' }),
};
