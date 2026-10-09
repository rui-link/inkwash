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
package top.ruilink.inkwash.system.service;

import java.util.List;

import top.ruilink.inkwash.base.enums.IdentityType;
import top.ruilink.inkwash.base.enums.IdentityVerifier;
import top.ruilink.inkwash.system.domain.SysIdentity;

/**
 * Identity claim service interface.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public interface IdentityService {
	SysIdentity findByTypeValue(IdentityType identityType, String identityValue);

	SysIdentity findByTypeProviderValue(IdentityType identityType, String provider, String identityValue);

	List<SysIdentity> listByUserId(Long userId);

	SysIdentity create(SysIdentity identity);

	void update(SysIdentity identity);

	void deleteById(Long id);

	/**
	 * Finds or creates a verified identity claim, in a concurrency safe way.
	 *
	 * An existing claim for the same user is reused, gaining verification details
	 * when it was unverified; an existing claim belonging to another user raises a
	 * taken exception; otherwise a verified claim is created, and a unique key
	 * conflict during creation triggers an automatic relookup and reuse.
	 */
	SysIdentity findOrCreateVerified(IdentityType type, String value, String provider, Long userId,
			IdentityVerifier by);
}
