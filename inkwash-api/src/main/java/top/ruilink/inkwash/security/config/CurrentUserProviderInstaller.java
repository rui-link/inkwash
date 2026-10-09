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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package top.ruilink.inkwash.security.config;

import org.springframework.stereotype.Component;

import top.ruilink.inkwash.base.util.CurrentUserProvider;
import top.ruilink.inkwash.security.util.SecurityUtil;

/**
 * Feeds {@link CurrentUserProvider} from {@link SecurityUtil} at startup.
 *
 * <p>
 * This is the only place where the dependency runs from {@code security} down
 * to {@code base}'s current-user abstraction. Platform code in {@code base}
 * calls {@code CurrentUserProvider} instead of {@code SecurityUtil}, which
 * restores the layering the design document requires (ISS-048).
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Component
public class CurrentUserProviderInstaller {

	public CurrentUserProviderInstaller() {
		CurrentUserProvider.install(SecurityUtil::getCurrentUserIdOrNull);
	}
}