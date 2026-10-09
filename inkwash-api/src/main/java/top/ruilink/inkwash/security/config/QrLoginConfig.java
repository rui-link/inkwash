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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.Data;

/**
 * QR code login configuration.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.qr-login")
public class QrLoginConfig {
	private String webUrl = "http://localhost:3001/qr-login";

	/**
	 * Comma separated Origin allowlist for the QR confirmation page; when empty
	 * only the webUrl origin itself is trusted.
	 */
	private String allowedOrigins = "";

	@Value("${cors.allowed-origin:}")
	private String corsAllowedOrigins = "";

	/**
	 * Checks a client reported Origin against the webUrl base origin,
	 * auth.qr-login.allowed-origins and the CORS allowlist. An Origin outside the
	 * allowlist is never used to build a QR code URL.
	 */
	public boolean isOriginAllowed(String origin) {
		if (!StringUtils.hasText(origin)) {
			return false;
		}
		String normalized = normalize(origin);
		for (String allowed : allAllowedOrigins()) {
			if (allowed.equals(normalized)) {
				return true;
			}
		}
		return false;
	}

	private List<String> allAllowedOrigins() {
		List<String> origins = new ArrayList<>();
		if (StringUtils.hasText(webUrl)) {
			try {
				URI uri = URI.create(webUrl);
				if (uri.getScheme() != null && uri.getHost() != null) {
					int port = uri.getPort();
					String base = uri.getScheme() + "://" + uri.getHost() + (port > 0 ? ":" + port : "");
					origins.add(base);
				}
			} catch (IllegalArgumentException e) {
				// An invalid webUrl is ignored
			}
		}
		origins.addAll(splitOrigins(allowedOrigins));
		origins.addAll(splitOrigins(corsAllowedOrigins));
		return origins;
	}

	private static List<String> splitOrigins(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty() && !s.contains("*"))
				.map(QrLoginConfig::normalize).toList();
	}

	private static String normalize(String origin) {
		return origin.replaceAll("/+$", "");
	}
}
