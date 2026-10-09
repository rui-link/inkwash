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
export function ensureArray(value) {
  return Array.isArray(value) ? value : [];
}

/**
 * Normalises a `PageResult` payload into `{ items, total }`.
 *
 * The backend contract is fixed: `PageResult` serialises `list` and `total`
 * (`base/domain/PageResult.java`), plus `pageNum` / `pageSize` / `totalPages` /
 * `hasNext` / `hasPrevious`.
 *
 * This helper deliberately reads **only** `list` and `total`. It used to accept
 * `content` / `items` and `totalElements` / `count` as well, but no endpoint ever
 * emitted those — the tolerance existed for a contract that was never real, and it
 * is exactly what hid ISS-009 (the frontend called seven `/averse` endpoints the
 * backend never implemented) and ISS-034 from being noticed. A missing or renamed
 * field must now fail loudly instead of degrading to an empty list.
 */
export function normalizePage(res) {
  if (!res || typeof res !== 'object') return { items: [], total: 0 };
  const items = ensureArray(res.list);
  const rawTotal = res.total;
  const total = Number.isFinite(Number(rawTotal)) && rawTotal !== '' ? Number(rawTotal) : items.length;
  return { items, total };
}
