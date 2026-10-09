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
package top.ruilink.inkwash.security.api;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import java.util.Arrays;
import java.util.Map;

/**
 * OAuth2 provider status REST endpoints.
 *
 * <p>
 * Reports whether each provider is actually wired up, rather than hardcoding
 * the answer. The frontend calls this to decide whether to render the "sign in
 * with GitHub" button, so a hardcoded {@code enabled: true} would offer a login
 * option that fails: OAuth2 providers only exist when the {@code oauth} profile
 * is active (see {@code application-oauth.yaml}).
 *
 * @author Dyllon
 * @since 0.5.1
 */
@RestController
@RequestMapping("/api/auth/oauth2")
@Validated
public class OAuth2Controller {

	/** Profile that registers the OAuth2 clients and their filter chain. */
	private static final String OAUTH_PROFILE = "oauth";

	private final Environment environment;

	public OAuth2Controller(Environment environment) {
		this.environment = environment;
	}

	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getOAuth2Status() {
		boolean githubEnabled = Arrays.asList(environment.getActiveProfiles()).contains(OAUTH_PROFILE);

		return ResponseEntity
				.ok(Map.of("github", Map.of("enabled", githubEnabled), "alipay", Map.of("enabled", false)));
	}
}
