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
package top.ruilink.inkwash.security.jwt;

import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.ruilink.inkwash.security.config.AuthCookieConfig;

/**
 * HttpOnly cookie token read and write (H-FS-3). Browser clients, detected by
 * X-Requested-With or a secure_* cookie, receive tokens through Set-Cookie on
 * the response.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Service
public class AuthCookieService {
	private final AuthCookieConfig properties;
	private final JwtTokenConfig jwtConfig;

	public AuthCookieService(AuthCookieConfig properties, JwtTokenConfig jwtConfig) {
		this.properties = properties;
		this.jwtConfig = jwtConfig;
	}

	/**
	 * Whether the current request is a browser request, carrying X-Requested-With:
	 * XMLHttpRequest or any secure_* cookie.
	 */
	public boolean isBrowserRequest(HttpServletRequest request) {
		String requestedWith = request.getHeader("X-Requested-With");
		if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
			return true;
		}
		return hasCookie(request, properties.getAccessCookie()) || hasCookie(request, properties.getRefreshCookie());
	}

	public void writeTokens(HttpServletResponse response, String accessToken, String refreshToken) {
		if (!properties.isEnabled()) {
			return;
		}
		response.addHeader("Set-Cookie",
				buildCookie(properties.getAccessCookie(), accessToken,
						resolveMaxAge(properties.getAccessMaxAge(), jwtConfig.getAccessExpireTime()),
						properties.getPath()).toString());
		response.addHeader("Set-Cookie",
				buildCookie(properties.getRefreshCookie(), refreshToken,
						resolveMaxAge(properties.getRefreshMaxAge(), jwtConfig.getRefreshExpireTime()),
						properties.getRefreshPath()).toString());
	}

	public void clearTokens(HttpServletResponse response) {
		if (!properties.isEnabled()) {
			return;
		}
		response.addHeader("Set-Cookie",
				buildCookie(properties.getAccessCookie(), "", 0, properties.getPath()).toString());
		response.addHeader("Set-Cookie",
				buildCookie(properties.getRefreshCookie(), "", 0, properties.getRefreshPath()).toString());
	}

	/** Reads the access token cookie, possibly null */
	public String readAccessToken(HttpServletRequest request) {
		return readCookie(request, properties.getAccessCookie());
	}

	/** Reads the refresh token cookie, possibly null */
	public String readRefreshToken(HttpServletRequest request) {
		return readCookie(request, properties.getRefreshCookie());
	}

	private String readCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private static long resolveMaxAge(Long overrideSeconds, Long ttlMillis) {
		if (overrideSeconds != null) {
			return overrideSeconds;
		}
		return ttlMillis != null ? TimeUnit.MILLISECONDS.toSeconds(ttlMillis) : 0;
	}

	private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds, String path) {
		return ResponseCookie.from(name, value).path(path).maxAge(maxAgeSeconds).httpOnly(true)
				.sameSite(properties.getSameSite()).secure(properties.isSecure()).build();
	}

	private boolean hasCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return false;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return true;
			}
		}
		return false;
	}
}
