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
package top.ruilink.inkwash.security.api.view;

/**
 * View of a linked login method.
 *
 * <p>
 * type separates identity claims from credentials: PHONE and EMAIL come from
 * sys_identity claims, while PASSWORD, OAUTH2 and QR_CODE come from sys_account
 * credential rows, which is how the frontend decides whether to unlink as
 * kind=IDENTITY or kind=ACCOUNT.
 * </p>
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record LinkedAccountView(Long id, String type, String value, String provider, Integer verified,
		String verifiedBy) {
}
