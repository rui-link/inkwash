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

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Cross-origin request policy and origin allowlist.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Configuration
public class CorsConfig {

	@Value("${cors.allowed-origin:http://localhost:9089,http://localhost:3000}")
	private String allowedOrigin;

	@Value("${cors.allowed-headers:*}")
	private List<String> allowedHeaders;

	@Value("${cors.allow-credentials:true}")
	private boolean allowCredentials;

	@Value("${cors.max-age:3600}")
	private long maxAge;

	/**
	 * The resolved allowlist, matched strictly as scheme://host[:port] and reused
	 * as the CSRF Origin allowlist (H-FS-3).
	 */
	public List<String> getAllowedOrigins() {
		return parseOrigins(allowedOrigin);
	}

	static List<String> parseOrigins(String raw) {
		return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.map(s -> s.replaceAll("/+$", "")).toList();
	}

	/**
	 * Rejects a wildcard entry at startup (ISS-062).
	 *
	 * <p>
	 * {@code cors.allowed-origin} does double duty: it is the CORS allowlist
	 * <em>and</em> the CSRF Origin allowlist consumed by
	 * {@code OriginCheckCsrfFilter}. That filter skips any allowlist entry
	 * containing {@code *}, so a wildcard does not "allow everything" — it removes
	 * that entry from CSRF checking entirely, while the configuration still looks
	 * like a populated allowlist and nothing fails.
	 *
	 * <p>
	 * Verified against the current configuration, which contains five exact origins
	 * and no wildcard. A wildcard here is always a misconfiguration rather than an
	 * intent: legitimate deployments need exact origins because credentials are
	 * allowed.
	 *
	 * @throws IllegalStateException if any entry contains a wildcard
	 */
	@PostConstruct
	void rejectWildcardOrigins() {
		List<String> wildcards = getAllowedOrigins().stream().filter(o -> o.contains("*")).toList();
		if (!wildcards.isEmpty()) {
			throw new IllegalStateException("cors.allowed-origin must not contain wildcards: " + wildcards
					+ ". 该配置同时用作 CSRF Origin 白名单；OriginCheckCsrfFilter 会跳过含 '*' 的项，"
					+ "等同于关闭 CSRF 校验且不会报错。请改为精确来源（如 http://localhost:9089）。");
		}
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origins = getAllowedOrigins();
		log.info("allowedOrigin: {}", origins);
		config.setAllowedOriginPatterns(origins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(allowedHeaders);
		config.setAllowCredentials(allowCredentials && !origins.contains("*"));
		config.setMaxAge(maxAge);
		config.setExposedHeaders(List.of("Authorization", "Access-Token", "Refresh-Token"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
