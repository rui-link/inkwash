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
package top.ruilink.inkwash.security.csrf;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.ruilink.inkwash.security.config.AuthCookieConfig;
import top.ruilink.inkwash.security.config.CorsConfig;

/**
 * H-FS-3 CSRF protection: a write carrying the secure_access cookie must
 * present an Origin that matches the cors.allowed-origin allowlist exactly, as
 * scheme://host[:port].
 *
 * @author Dyllon
 * @since 0.5.1
 */
public class OriginCheckCsrfFilter extends OncePerRequestFilter {
	private final CorsConfig corsConfig;
	private final AuthCookieConfig cookieProperties;

	public OriginCheckCsrfFilter(CorsConfig corsConfig, AuthCookieConfig cookieProperties) {
		this.corsConfig = corsConfig;
		this.cookieProperties = cookieProperties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!cookieProperties.isEnabled() || !cookieProperties.isCsrfCheckEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}
		if (!isBrowserWrite(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		String origin = request.getHeader("Origin");
		if (origin == null || origin.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}
		if (originAllowed(origin)) {
			filterChain.doFilter(request, response);
			return;
		}
		reject(response);
	}

	/**
	 * A browser mode write, meaning a non-read-only request carrying the
	 * secure_access cookie
	 */
	private boolean isBrowserWrite(HttpServletRequest request) {
		if (isReadOnly(request.getMethod())) {
			return false;
		}
		return hasCookie(request, cookieProperties.getAccessCookie());
	}

	private boolean isReadOnly(String method) {
		return HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method);
	}

	private boolean originAllowed(String origin) {
		String normalized = normalize(origin);
		if (normalized == null) {
			return false;
		}
		for (String allowed : corsConfig.getAllowedOrigins()) {
			if (allowed.indexOf('*') >= 0) {
				continue;
			}
			if (normalized.equals(normalize(allowed))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Normalises to scheme://host[:port]: lower case, explicit port, with http:80
	 * and https:443 treated as no port. An invalid URI returns null and is treated
	 * as no match.
	 */
	private String normalize(String origin) {
		if (origin == null) {
			return null;
		}
		URI uri;
		try {
			uri = URI.create(origin.trim());
		} catch (IllegalArgumentException e) {
			return null;
		}
		String scheme = uri.getScheme();
		String host = uri.getHost();
		if (scheme == null || host == null) {
			return null;
		}
		scheme = scheme.toLowerCase();
		host = host.toLowerCase();
		int port = uri.getPort();
		if (port < 0 || ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
			return scheme + "://" + host;
		}
		return scheme + "://" + host + ":" + port;
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

	private void reject(HttpServletResponse response) throws IOException {
		response.setContentType("application/json;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"CSRF origin mismatch\"}");
	}
}
