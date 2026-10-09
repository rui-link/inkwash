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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import top.ruilink.inkwash.security.csrf.OriginCheckCsrfFilter;
import top.ruilink.inkwash.security.handler.OAuth2LoginSuccessHandler;
import top.ruilink.inkwash.security.jwt.JwtAuthenticationFilter;
import top.ruilink.inkwash.security.service.impl.OAuth2UserServiceImpl;
import top.ruilink.inkwash.base.util.LocaleUtil;

/**
 * HTTP security filter chain configuration.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OriginCheckCsrfFilter originCheckCsrfFilter;
	private final CorsConfigurationSource corsConfigurationSource;
	private final OAuth2UserServiceImpl customOAuth2UserService;
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, OriginCheckCsrfFilter originCheckCsrfFilter,
			CorsConfigurationSource corsConfigurationSource, OAuth2UserServiceImpl customOAuth2UserService,
			OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.originCheckCsrfFilter = originCheckCsrfFilter;
		this.corsConfigurationSource = corsConfigurationSource;
		this.customOAuth2UserService = customOAuth2UserService;
		this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**").csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((_, response, _) -> {
					response.setContentType("application/json;charset=utf-8");
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\""
							+ LocaleUtil.getValue("error.auth.unauthorized").replace("\"", "\\\"") + "\"}");
				})).authorizeHttpRequests(auth -> auth
						// Public CMS GET endpoints for articles, categories and terms
						.requestMatchers(HttpMethod.GET, "/api/cms/articles", "/api/cms/articles/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/cms/categories", "/api/cms/terms").permitAll()
						// Public system metadata endpoints exposing non-sensitive build info such as
						// product name and version
						.requestMatchers(HttpMethod.GET, "/api/system/meta").permitAll()
						// Public license endpoints, needed by the login page to check license status
						// /info is included because the admin frontend fetches it during bootstrap,
						// before anyone has signed in; it reports status fields to everyone but the
						// holder's email is redacted for anonymous callers (see LicenseController).
						.requestMatchers(HttpMethod.GET, "/api/license/public-key", "/api/license/health",
								"/api/license/info")
						.permitAll()
						// CMS management endpoints, authentication required.
						// The former "/api/cms/admin/**" matcher was removed (ISS-056): no controller
						// maps that prefix, and "/api/cms/**" already subsumes it.
						.requestMatchers("/api/cms/**").authenticated()
						// Public authentication endpoints
						.requestMatchers("/api/auth/qr/**").permitAll()
						// Only GET exists on /api/auth/captcha (AuthController) and the frontend calls
						// it with GET; the former POST permitAll rule matched nothing (ISS-056).
						.requestMatchers(HttpMethod.GET, "/api/auth/captcha").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/sms/code").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/register/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/token/refresh").permitAll()
						// OAuth2 callback page posting a message to the parent window
						.requestMatchers(HttpMethod.GET, "/api/auth/oauth2/token").permitAll().anyRequest()
						.authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(originCheckCsrfFilter, JwtAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * OAuth2 login/callback chain.
	 *
	 * <p>
	 * Bound to the {@code oauth} profile. {@code oauth2Login()} requires a
	 * {@code ClientRegistrationRepository}; with no
	 * {@code spring.security.oauth2.client.registration.*} entry present — the
	 * default, since OAuth credentials are opt-in via
	 * {@code application-oauth.yaml} — Spring Boot's OAuth2 client
	 * auto-configuration backs off and no such bean exists. Wiring
	 * {@code oauth2Login()} unconditionally would then fail the whole context at
	 * startup, taking password login down with it over a feature the operator never
	 * asked for.
	 */
	@Bean
	@Profile("oauth")
	@Order(Ordered.LOWEST_PRECEDENCE - 50)
	public SecurityFilterChain oauth2ClientRedirectFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/oauth2/**", "/login/**", "/.well-known/**").csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.oauth2Login(oauth2 -> oauth2.redirectionEndpoint(endpoint -> endpoint.baseUri("/oauth2/callback/*"))
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
						.successHandler(oAuth2LoginSuccessHandler));
		return http.build();
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE - 40)
	public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/actuator/**").csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(
						auth -> auth.requestMatchers("/actuator/health/**").permitAll().anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
