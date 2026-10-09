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
package top.ruilink.inkwash.system.api.view;

import lombok.Data;

/**
 * Result view for an administrator resetting a user's password.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
public class ResetPasswordView {

	private Long userId;

	/**
	 * Whether the new password was emailed to the user, false when mail is
	 * unconfigured or sending failed
	 */
	private boolean emailSent;

	/**
	 * One-time temporary password shown only in this response body, so an
	 * administrator can read it out to the user when mail is unavailable. It must
	 * never be stored or logged after the response is returned.
	 */
	private String password;
}
