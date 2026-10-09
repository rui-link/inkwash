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
package top.ruilink.inkwash.cmd.license;

/**
 * Single source of license defaults and edition limits for the CLI.
 *
 * NOTE: the api module keeps its own copy of the edition limits
 * (top.ruilink.inkwash.security.license.TierService, TRIAL -> 10 users). That
 * copy is intentionally left untouched; keep this file in sync manually.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class LicenseConstants {

	private LicenseConstants() {
	}

	/**
	 * Default validity in days applied to TRIAL licenses that specify no --days.
	 */
	public static final int DEFAULT_TRIAL_DAYS = 30;

	public static final int PERSONAL_MAX_USERS = 30;
	public static final int PROFESSIONAL_MAX_USERS = 100;
	public static final int TRIAL_MAX_USERS = 10;

	/**
	 * Max users for an edition name (lower-case). Enterprise is unlimited (null).
	 *
	 * @throws IllegalArgumentException for unknown editions
	 */
	public static Integer maxUsersForEdition(String edition) {
		return switch (edition == null ? "" : edition.trim().toLowerCase()) {
		case "personal" -> PERSONAL_MAX_USERS;
		case "professional" -> PROFESSIONAL_MAX_USERS;
		case "enterprise" -> null;
		case "trial" -> TRIAL_MAX_USERS;
		default -> throw new IllegalArgumentException(
				"Unknown edition '" + edition + "'. Use personal, professional, enterprise, or trial.");
		};
	}
}
