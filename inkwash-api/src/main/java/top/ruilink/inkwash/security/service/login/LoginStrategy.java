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
package top.ruilink.inkwash.security.service.login;

import top.ruilink.inkwash.security.api.param.LoginParam;

/**
 * Login strategy interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface LoginStrategy {

	/**
	 * Whether this strategy supports the given login parameter type.
	 */
	boolean supports(LoginParam loginParam);

	/**
	 * Performs the login validation.
	 */
	LoginResult authenticate(LoginParam loginParam);
}
