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

import java.time.LocalDateTime;
import java.util.List;

/**
 * License information view for showing the deployment license status in the
 * frontend.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public record LicenseInfoView(boolean licensed, String status, String message, String warning,
		LicenseDataView licenseData, String edition, Integer currentUserCount, Integer maxUsers, boolean tierExceeded,
		List<String> allowedModules) {

	/**
	 * Detailed information about the deployed license.
	 */
	public record LicenseDataView(String subject, String holder, String email, String type, LocalDateTime issuedDate,
			LocalDateTime expirationDate, Integer maxUsers, List<String> modules) {
	}
}
