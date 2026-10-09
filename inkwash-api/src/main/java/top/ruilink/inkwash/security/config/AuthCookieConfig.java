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
package top.ruilink.inkwash.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * HttpOnly cookie authentication configuration (H-FS-3); tokens never pass
 * through JS and are delivered only in the cookies named here.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieConfig {
	/**
	 * Whether cookie authentication is enabled; disabling it reverts the backend to
	 * the pure Authorization contract
	 */
	private boolean enabled = true;
	/** Whether the Origin allowlist CSRF check is enabled */
	private boolean csrfCheckEnabled = true;
	/** Access token cookie name */
	private String accessCookie = "secure_access";
	/** Refresh token cookie name */
	private String refreshCookie = "secure_refresh";
	/** Access cookie path */
	private String path = "/";
	/** Refresh cookie path, used only by /api/auth */
	private String refreshPath = "/api/auth";
	/** SameSite policy */
	private String sameSite = "Lax";
	/** Secure flag, enabled in production */
	private boolean secure = false;
	/**
	 * Explicit access cookie Max-Age in seconds, defaulting to the JWT access TTL
	 * when unset
	 */
	private Long accessMaxAge;
	/**
	 * Explicit refresh cookie Max-Age in seconds, defaulting to the JWT refresh TTL
	 * when unset
	 */
	private Long refreshMaxAge;
}
