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
package top.ruilink.inkwash.security.license;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.system.mapper.UserMapper;

/**
 * Tier Service - Encapsulates license tier logic Determines allowed modules
 * based on edition and user count
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Service
public class TierService {

	private static final List<String> ALL_MODULES = List.of("system", "cms", "monitor");

	private final UserMapper userMapper;

	public TierService(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	/**
	 * Tier/edition definitions with user limits
	 */
	public enum Edition {
		PERSONAL("personal", 30), PROFESSIONAL("professional", 100), ENTERPRISE("enterprise", null), TRIAL("trial", 10);

		private final String name;
		private final Integer maxUsers;

		Edition(String name, Integer maxUsers) {
			this.name = name;
			this.maxUsers = maxUsers;
		}

		public String getName() {
			return name;
		}

		public Integer getMaxUsers() {
			return maxUsers;
		}
	}

	/**
	 * Result of a tier access check
	 */
	public record TierCheckResult(boolean allowed, boolean userLimitExceeded, int currentUserCount, Integer maxUsers,
			String edition, List<String> allowedModules) {
	}

	/**
	 * Check if a module access is allowed based on license tier and user count
	 */
	public TierCheckResult checkAccess(LicenseData license, String requestedModule) {
		Edition edition = parseEdition(license != null ? license.getEdition() : null);

		int currentUserCount = (int) userMapper.count();

		// Priority: license.maxUsers > edition.maxUsers
		// license.maxUsers = 0 or null means use edition default
		Integer effectiveMaxUsers = null;
		if (license != null && license.getMaxUsers() != null && license.getMaxUsers() > 0) {
			effectiveMaxUsers = license.getMaxUsers();
		} else {
			effectiveMaxUsers = edition.getMaxUsers();
		}

		boolean userLimitExceeded = effectiveMaxUsers != null && currentUserCount > effectiveMaxUsers;

		List<String> allowedModules = getAllowedModules(edition, userLimitExceeded);

		boolean allowed = requestedModule == null || allowedModules.contains(requestedModule);

		log.debug(
				"Tier check: edition={}, licenseMaxUsers={}, effectiveMaxUsers={}, users={}/{}, exceeded={}, module={}, allowed={}",
				edition.name, license != null ? license.getMaxUsers() : null, effectiveMaxUsers, currentUserCount,
				effectiveMaxUsers, userLimitExceeded, requestedModule, allowed);

		return new TierCheckResult(allowed, userLimitExceeded, currentUserCount, effectiveMaxUsers, edition.name,
				allowedModules);
	}

	private Edition parseEdition(String editionStr) {
		if (editionStr == null || editionStr.isEmpty()) {
			log.warn("No edition specified in license, defaulting to PROFESSIONAL");
			return Edition.PROFESSIONAL;
		}
		try {
			return Edition.valueOf(editionStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			log.warn("Unknown edition '{}', defaulting to PROFESSIONAL", editionStr);
			return Edition.PROFESSIONAL;
		}
	}

	private List<String> getAllowedModules(Edition edition, boolean exceeded) {
		if (!exceeded) {
			return ALL_MODULES;
		}
		return switch (edition) {
		case PERSONAL, TRIAL -> List.of("system");
		case PROFESSIONAL -> List.of("system", "cms");
		case ENTERPRISE -> ALL_MODULES;
		};
	}
}
