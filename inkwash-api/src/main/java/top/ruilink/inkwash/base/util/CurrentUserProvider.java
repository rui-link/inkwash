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
package top.ruilink.inkwash.base.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Access to the identity of the caller, for platform code that must not depend
 * on {@code security}.
 *
 * <p>
 * {@code FileUploadServiceImpl} (namespaces stored files by uploader) and
 * {@code DataMaskSerializer} (self-view exemption) both need the current user
 * id, but both live in {@code base}, which the design document forbids from
 * depending on an upper layer. They used to call {@code SecurityUtil} directly,
 * inverting the dependency (ISS-048).
 *
 * <p>
 * The direction is now inverted: {@code base} declares this abstraction, and
 * the {@code security} layer installs an implementation at startup. This
 * mirrors how {@link LocaleUtil} is wired — a static holder fed by a Spring
 * bean from the owning module.
 *
 * <p>
 * Spring Security itself is a framework dependency, not a project-layer one, so
 * reading {@code SecurityContextHolder} from {@code base} does not reintroduce
 * the coupling; only the project's own {@code security} types stay out.
 *
 * @author Dyllon
 * @since 0.5.1
 */
public final class CurrentUserProvider {

	private static volatile java.util.function.Supplier<Long> userIdSupplier;

	private CurrentUserProvider() {
	}

	/**
	 * Installs the supplier used by {@link #getCurrentUserIdOrNull()}.
	 *
	 * <p>
	 * Called once by the {@code security} module so that {@code base} never has to
	 * import it. Passing {@code null} restores the anonymous default.
	 */
	public static void install(java.util.function.Supplier<Long> supplier) {
		userIdSupplier = supplier;
	}

	/**
	 * @return the current user id, or {@code null} when nobody is authenticated or
	 *         no provider is installed
	 */
	public static Long getCurrentUserIdOrNull() {
		java.util.function.Supplier<Long> supplier = userIdSupplier;
		return supplier == null ? null : supplier.get();
	}

	/**
	 * @return {@code true} when an authenticated principal is present
	 */
	public static boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated();
	}
}