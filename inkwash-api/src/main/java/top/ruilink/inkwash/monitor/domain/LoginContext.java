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
package top.ruilink.inkwash.monitor.domain;

import top.ruilink.inkwash.base.enums.AuthType;

/**
 * Login context holding the authenticated user, principal, auth type, client IP
 * and User-Agent.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record LoginContext(Long userId, String subject, AuthType authType, String ip, String userAgent) {

	public LoginContext(Long userId, String subject, AuthType authType) {
		this(userId, subject, authType, null, null);
	}
}
