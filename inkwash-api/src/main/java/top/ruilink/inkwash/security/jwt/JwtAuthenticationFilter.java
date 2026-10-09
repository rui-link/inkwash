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

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.util.LocaleUtil;
import top.ruilink.inkwash.security.adapter.SysUserDetailsService;
import top.ruilink.inkwash.security.service.TokenService;

/**
 * JWT authentication filter validating the token on every request and injecting
 * the authentication into the security context. Extends OncePerRequestFilter so
 * it runs once per request.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider tokenProvider;
	private final SysUserDetailsService userDetailsService;
	private final TokenService tokenService;
	private final AuthCookieService cookieService;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, SysUserDetailsService userDetailsService,
			TokenService tokenService, AuthCookieService cookieService) {
		this.tokenProvider = tokenProvider;
		this.userDetailsService = userDetailsService;
		this.tokenService = tokenService;
		this.cookieService = cookieService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// 1. Read the token from the header, falling back to the secure_access cookie
		// when absent (H-FS-3 browser mode)
		String token = tokenProvider.extractTokenFromRequest(request);
		if (token == null) {
			token = cookieService.readAccessToken(request);
		}

		// 2. Validate the token when one is present and the request is not yet
		// authenticated
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			// Check that the token is still valid
			if (tokenProvider.validateToken(token)) {
				// Check whether the token has been revoked by a logout
				if (tokenService.isTokenRevoked(token)) {
					log.warn("令牌已撤销，请求URI：{}", request.getRequestURI());
					response.setContentType("application/json;charset=utf-8");
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\""
							+ LocaleUtil.getValue("error.auth.token_revoked").replace("\"", "\\\"") + "\"}");
					return;
				}

				// Parse the token for the username and load the user details
				String username = tokenProvider.extractUsername(token);
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);

				// Wrap the authentication and place it in the security context, using
				// UserDetails as principal so full information is retained
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);

				log.debug("JWT令牌验证成功，用户{}已注入Security上下文", username);
			} else {
				log.warn("JWT令牌无效，请求URI：{}", request.getRequestURI());
			}
		}

		// 3. Continue the filter chain, where permitAll rules in SecurityConfig control
		// the public endpoints
		filterChain.doFilter(request, response);
	}
}
