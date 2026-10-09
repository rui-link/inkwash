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
package top.ruilink.inkwash.security.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import top.ruilink.inkwash.base.enums.AuthType;
import top.ruilink.inkwash.base.util.ClientInfoUtil;
import top.ruilink.inkwash.monitor.domain.LoginContext;
import top.ruilink.inkwash.monitor.domain.LoginEvent;
import top.ruilink.inkwash.security.api.view.AccountView;
import top.ruilink.inkwash.security.service.OAuth2LoginService;
import top.ruilink.inkwash.security.service.TokenService;
import top.ruilink.inkwash.security.service.login.LoginResult;

/**
 * OAuth2 login success handler issuing tokens and redirecting.
 *
 * @author Dyllon
 * @since 0.5.1
 */
@Slf4j
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final OAuth2LoginService oauth2LoginService;
	private final TokenService tokenService;
	private final ApplicationEventPublisher eventPublisher;

	public OAuth2LoginSuccessHandler(OAuth2LoginService oauth2LoginService, TokenService tokenService,
			ApplicationEventPublisher eventPublisher) {
		this.oauth2LoginService = oauth2LoginService;
		this.tokenService = tokenService;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
			response.sendRedirect("/login?error=invalid_oauth");
			return;
		}

		String registrationId = oauthToken.getAuthorizedClientRegistrationId();
		OAuth2User oauth2User = oauthToken.getPrincipal();
		Map<String, Object> attributes = oauth2User.getAttributes();

		String openId = extractOpenId(registrationId, attributes);
		String verifiedEmail = (String) attributes.get("verified_email");
		String accessToken = (String) attributes.get("oauth_access_token");
		String refreshToken = (String) attributes.get("oauth_refresh_token");
		Object name = attributes.getOrDefault("name", registrationId);

		log.info("OAuth2登录成功, registrationId={}, openId={}, name={}", registrationId, openId, oauth2User.getName());

		LoginResult result = oauth2LoginService.login(registrationId, openId, name == null ? null : name.toString(),
				verifiedEmail, accessToken, refreshToken);

		AccountView accountView = tokenService.generateTokens(result.userId(), result.subject(),
				AuthType.OAUTH2.getCode());
		String token = accountView.getCredential();

		eventPublisher.publishEvent(
				new LoginEvent(oauthToken.getPrincipal(), new LoginContext(result.userId(), result.subject(),
						AuthType.OAUTH2, ClientInfoUtil.getClientIp(request), request.getHeader("User-Agent"))));

		String callbackUrl = "/api/auth/oauth2/token?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
		response.sendRedirect(callbackUrl);
	}

	private String extractOpenId(String provider, Map<String, Object> attrs) {
		return switch (provider.toLowerCase()) {
		case "github" -> {
			Object id = attrs.get("id");
			yield id != null ? id.toString() : attrs.getOrDefault("login", "unknown").toString();
		}
		case "alipay" -> attrs.getOrDefault("user_id", attrs.getOrDefault("openid", "unknown")).toString();
		default -> attrs.getOrDefault("id", attrs.getOrDefault("sub", "unknown")).toString();
		};
	}
}
